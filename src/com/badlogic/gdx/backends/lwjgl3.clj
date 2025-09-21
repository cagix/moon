(ns com.badlogic.gdx.backends.lwjgl3
  (:import (com.badlogic.gdx ApplicationListener
                             Gdx)
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

(defn- ->Lwjgl3ApplicationConfiguration [config]
  (let [obj (Lwjgl3ApplicationConfiguration.)]
    (doseq [[k v] config]
      (set-config-key! obj k v))
    obj))

(defn- ->ApplicationListener
  [{:keys [create
           dispose
           render
           resize
           pause
           resume]}]
  (reify ApplicationListener
    (create [_]
      (create {:gdx/app      Gdx/app
               :gdx/audio    Gdx/audio
               :gdx/files    Gdx/files
               :gdx/graphics Gdx/graphics
               :gdx/input    Gdx/input}))
    (dispose [_]
      (dispose))
    (render [_]
      (render))
    (resize [_ width height]
      (resize width height))
    (pause [_]
      (pause))
    (resume [_]
      (resume))))

(defn application!
  [listener config]
  (Lwjgl3Application. (->ApplicationListener listener)
                      (->Lwjgl3ApplicationConfiguration config)))
