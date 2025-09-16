(ns clojure.gdx.backends.lwjgl
  (:import (com.badlogic.gdx ApplicationListener)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration
                                             Lwjgl3WindowConfiguration)))

(defn- set-window-config-key! [^Lwjgl3WindowConfiguration object k v]
  (case k
    :windowed-mode (.setWindowedMode object
                                     (int (:width v))
                                     (int (:height v)))
    :title (.setTitle object (str v))))

(defn- set-config-key! [^Lwjgl3ApplicationConfiguration object k v]
  (case k
    :foreground-fps (.setForegroundFPS object (int v))
    (set-window-config-key! object k v)))

(defn- create-config [config]
  (let [obj (Lwjgl3ApplicationConfiguration.)]
    (doseq [[k v] config]
      (set-config-key! obj k v))
    obj))

(defn- create-listener
  [{:keys [create!
           dispose!
           render!
           resize!
           pause!
           resume!]}]
  (reify ApplicationListener
    (create [_]
      (create!))
    (dispose [_]
      (dispose!))
    (render [_]
      (render!))
    (resize [_ width height]
      (resize! width height))
    (pause [_]
      (pause!))
    (resume [_]
      (resume!))))

(defn start-application! [listener config]
  (let [application (Lwjgl3Application.)]
    (.start application
            (create-listener listener)
            (create-config config))))

(defn start!
  [{:keys [ctx/application-state]
    :as ctx}
   {:keys [listener
           config]}]
  (reset! application-state ctx)
  (start-application! (let [[f params] listener]
                        (f params))
                      config))
