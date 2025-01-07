(ns gdl.test)

(import 'com.badlogic.gdx.backends.lwjgl3.Lwjgl3NativesLoader)
(import 'org.lwjgl.glfw.GLFW)
(import 'org.lwjgl.system.Configuration)

(defn- init-glfw []
  (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
  (.set Configuration/GLFW_CHECK_THREAD0 false)
  (Lwjgl3NativesLoader/load)
  ; errorCallback = GLFWErrorCallback.createPrint(Lwjgl3ApplicationConfiguration.errorStream);
  ; GLFW.glfwSetErrorCallback(errorCallback);
  (when-not (GLFW/glfwInit)
    (throw (Exception. "Unable to init GLFW."))))

(defn -main []
  (init-glfw)
  (println "Initialized GLFW")
  (Thread/sleep 3000)
  (println "slept 3 seconds"))
