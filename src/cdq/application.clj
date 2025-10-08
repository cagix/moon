(ns cdq.application
  (:require clojure.lwjgl.system.configuration
            [cdq.game.create :as create]
            [cdq.game.dispose :as dispose]
            [cdq.game.render :as render]
            [cdq.game.resize :as resize]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.badlogic.gdx]
            [com.badlogic.gdx.backends.lwjgl]
            [qrecord.core :as q])
  (:gen-class))

(q/defrecord Context [])

(def state (atom nil))

(defn edn-resource [path]
  (->> path
       io/resource
       slurp
       (edn/read-string {:readers {'edn/resource edn-resource}})))

(def config (edn-resource "config.edn"))

(defn -main []
  (com.badlogic.gdx/def-colors! {"PRETTY_NAME" [0.84 0.8 0.52 1]})
  (clojure.lwjgl.system.configuration/set-glfw-library-name! "glfw_async")
  (com.badlogic.gdx.backends.lwjgl/application
   {
    :title "Cyber Dungeon Quest"

    :window {:width 1440
             :height 900}

    :fps 60

    :create! (fn []
               (reset! state (create/do! (assoc (map->Context {})
                                                :ctx/gdx (com.badlogic.gdx/context))
                                         config)))

    :dispose! (fn []
                (dispose/do! @state))

    :render! (fn []
               (swap! state render/do!))

    :resize! (fn [width height]
               (resize/do! @state width height))
    }))
