// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.jpsBootstrap;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.io.URLUtil;
import org.jetbrains.jps.model.JpsElementFactory;
import org.jetbrains.jps.model.JpsModel;
import org.jetbrains.jps.model.java.JpsJavaDependenciesEnumerator;
import org.jetbrains.jps.model.java.JpsJavaExtensionService;
import org.jetbrains.jps.model.library.JpsLibrary;
import org.jetbrains.jps.model.library.JpsOrderRootType;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.serialization.JpsModelSerializationDataService;
import org.jetbrains.jps.model.serialization.JpsPathVariablesConfiguration;
import org.jetbrains.jps.model.serialization.JpsProjectLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@SuppressWarnings("SameParameterValue")
public class JpsProjectUtils {
  public static JpsModel loadJpsProject(Path projectHome) throws Exception {
    long startTime = System.currentTimeMillis();

    Path m2LocalRepository = Path.of(System.getProperty("user.home"), ".m2", "repository");
    JpsModel model = JpsElementFactory.getInstance().createModel();
    JpsPathVariablesConfiguration pathVariablesConfiguration =
      JpsModelSerializationDataService.getOrCreatePathVariablesConfiguration(model.getGlobal());
    pathVariablesConfiguration.addPathVariable(
      "MAVEN_REPOSITORY", FileUtilRt.toSystemIndependentName(m2LocalRepository.toAbsolutePath().toString()));

    Map<String, String> pathVariables = JpsModelSerializationDataService.computeAllPathVariables(model.getGlobal());
    JpsProjectLoader.loadProject(model.getProject(), pathVariables, projectHome.toString());
    System.out.println(
      "Loaded project " + projectHome + ": " +
        model.getProject().getModules().size() + " modules, " +
        model.getProject().getLibraryCollection().getLibraries().size() + " libraries in " +
        (System.currentTimeMillis() - startTime) + " ms");

    addSdk(model, "corretto-11", System.getProperty("java.home"));

    return model;
  }

  public static JpsModule getModuleByName(JpsModel model, String moduleName) {
    return model.getProject().getModules()
      .stream()
      .filter(m -> moduleName.equals(m.getName()))
      .findFirst().orElseThrow(() -> new IllegalStateException("Module " + moduleName + " is not found"));
  }

  public static List<URL> getModuleRuntimeClasspath(JpsModule module) throws MalformedURLException {
    JpsJavaDependenciesEnumerator enumerator = JpsJavaExtensionService
      .dependencies(module)
      .runtimeOnly()
      .productionOnly()
      .recursively()
      .withoutSdk();

    List<URL> roots = new ArrayList<>();
    for (File file : enumerator.classes().getRoots()) {
      URL toURL = file.toURI().toURL();
      roots.add(toURL);
    }
    roots.sort(Comparator.comparing(URL::toString));

    return roots;
  }

  private static void addSdk(JpsModel model, String sdkName, String sdkHome) throws IOException {
    JpsJavaExtensionService.getInstance().addJavaSdk(model.getGlobal(), sdkName, sdkHome);
    JpsLibrary additionalSdk = model.getGlobal().getLibraryCollection().findLibrary(sdkName);
    if (additionalSdk == null) {
      throw new IllegalStateException("SDK " + sdkHome + " was not found");
    }

    for (String moduleUrl : readModulesFromReleaseFile(Path.of(sdkHome))) {
      additionalSdk.addRoot(moduleUrl, JpsOrderRootType.COMPILED);
    }
  }

  private static List<String> readModulesFromReleaseFile(Path jdkDir) throws IOException {
    Path releaseFile = jdkDir.resolve("release");
    Properties p = new Properties();
    try (InputStream is = Files.newInputStream(releaseFile)) {
      p.load(is);
    }
    String jbrBaseUrl = URLUtil.JRT_PROTOCOL + URLUtil.SCHEME_SEPARATOR +
      FileUtil.toSystemIndependentName(jdkDir.toFile().getAbsolutePath()) +
      URLUtil.JAR_SEPARATOR;
    String modules = p.getProperty("MODULES");
    return ContainerUtil.map(StringUtil.split(StringUtil.unquoteString(modules), " "), s -> jbrBaseUrl + s);
  }
}
