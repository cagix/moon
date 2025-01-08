(ns cdq.start
  (:require [clojure.edn :as edn]
            [clojure.gdx.application :as application]
            [clojure.gdx.backends.lwjgl3.application :as lwjgl3-app]
            [clojure.java.io :as io]
            [cdq.app :as game])
  (:gen-class))

(def state (atom nil))

(defn -main []
  (let [config (-> "app.edn" io/resource slurp edn/read-string)]
    (lwjgl3-app/create (reify application/Listener
                         (create [_ context]
                           (reset! state (game/create context (:context config))))

                         (dispose [_]
                           (game/dispose @state))

                         (pause [_])

                         (render [_]
                           (swap! state game/render))

                         (resize [_ width height]
                           (game/resize @state width height))

                         (resume [_]))
                       (:app config))))

(extend-type com.badlogic.gdx.Files
  clojure.files/Files
  (internal [this path]
    (.internal this path)))

(extend-type com.badlogic.gdx.Graphics
  clojure.graphics/Graphics
  (delta-time [this]
    (.getDeltaTime this))
  (frames-per-second [this]
    (.getFramesPerSecond this))
  (new-cursor [this pixmap hotspot-x hotspot-y]
    (.newCursor this pixmap hotspot-x hotspot-y))
  (set-cursor [this cursor]
    (.setCursor this cursor)))
