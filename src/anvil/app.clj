(ns anvil.app
  (:require [clojure.awt :as awt]
            [clojure.component :as component]
            [clojure.gdx.app :as app]
            [clojure.gdx.audio.sound :as sound]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.gdx.utils.shared-library-loader :as shared-library-loader]
            [clojure.lwjgl :as lwjgl]
            [clojure.utils :refer [bind-root]]))

(defn start [{:keys [components] :as config}]
  (awt/set-dock-icon (:dock-icon config))
  (when shared-library-loader/mac?
    (lwjgl/configure-glfw-for-mac))
  (lwjgl3/app (reify lwjgl3/Listener
                (create  [_]     (run! component/create          components))
                (dispose [_]     (run! component/dispose         components))
                (render  [_]     (run! component/render          components))
                (resize  [_ w h] (run! #(component/resize % w h) components)))
              (lwjgl3/config (:lwjgl3 config))))

(def exit app/exit)

(defmacro post-runnable [& exprs]
  `(app/post-runnable (fn [] ~@exprs)))

(declare screens
         current-screen-key)

(defn current-screen []
  (and (bound? #'current-screen-key)
       (current-screen-key screens)))

(defn change-screen
  "Calls `exit` on the current-screen and `enter` on the new screen."
  [new-k]
  (when-let [screen (current-screen)]
    (component/exit screen))
  (let [screen (new-k screens)]
    (assert screen (str "Cannot find screen with key: " new-k))
    (bind-root current-screen-key new-k)
    (component/enter screen)))

(declare assets)

(defn play-sound [sound-name]
  (->> sound-name
       (format "sounds/%s.wav")
       assets
       sound/play))
