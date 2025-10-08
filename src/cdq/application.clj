(ns cdq.application
  (:require clojure.lwjgl.system.configuration
            [cdq.game.create.record :as create-record]
            [cdq.game.create.get-gdx :as get-gdx]
            [cdq.game.create.tx-handler :as create-tx-handler]
            [cdq.game.create.db :as create-db]
            [cdq.game.create.graphics :as create-graphics]
            [cdq.game.create.ui :as create-ui]
            [cdq.game.create.input-processor :as create-input-processor]
            [cdq.game.create.audio :as create-audio]
            [cdq.game.create.world :as create-world]
            [cdq.game.dispose :as dispose]
            [cdq.game.render :as render]
            [cdq.game.resize :as resize]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.gdx.graphics.colors]
            [com.badlogic.gdx.backends.lwjgl])
  (:gen-class))

(def state (atom nil))

(defn edn-resource [path]
  (->> path
       io/resource
       slurp
       (edn/read-string {:readers {'edn/resource edn-resource}})))

(def config (edn-resource "config.edn"))

(defn -main []
  (clojure.gdx.graphics.colors/put! {"PRETTY_NAME" [0.84 0.8 0.52 1]})
  (clojure.lwjgl.system.configuration/set-glfw-library-name! "glfw_async")
  (com.badlogic.gdx.backends.lwjgl/application
   {
    :title "Cyber Dungeon Quest"

    :window {:width 1440
             :height 900}

    :fps 60

    :create! (fn []
               (reset! state (-> {}
                                 create-record/do!
                                 get-gdx/do!
                                 create-tx-handler/do!
                                 create-db/do!
                                 (create-graphics/do! (:graphics config))
                                 (create-ui/do! (:ui config))
                                 create-input-processor/do!
                                 (create-audio/do! (:audio config))
                                 (create-world/do! (:world config)))))

    :dispose! (fn []
                (dispose/do! @state))

    :render! (fn []
               (swap! state render/do!))

    :resize! (fn [width height]
               (resize/do! @state width height))
    }))
