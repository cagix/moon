(ns cdq.application
  (:require cdq.game.create
            cdq.game.dispose
            cdq.game.render
            cdq.game.resize
            cdq.ui.editor.schemas-impl
            [clojure.config :as config]
            [clojure.gdx :as gdx]
            [clojure.gdx.application.listener :as listener]
            [clojure.gdx.backends.lwjgl.application :as application]
            [clojure.gdx.backends.lwjgl.application.config :as app-config]
            [clojure.lwjgl.system.configuration :as lwjgl-config])
  (:gen-class))

(defn- create! []
  (cdq.game.create/pipeline (gdx/context)
                            (config/edn-resource "create-pipeline.edn")))

(def state (atom nil))

(defn -main []
  (let [{:keys [config]} (config/edn-resource "config.edn")]
    (lwjgl-config/set-glfw-library-name! "glfw_async")
    (application/create (listener/create
                         (let [render-pipeline (config/edn-resource "render-pipeline.edn")]
                           {:create (fn []
                                      (reset! state (create!)))
                            :dispose (fn []
                                       (cdq.game.dispose/do! @state))
                            :render (fn []
                                      (swap! state cdq.game.render/do! render-pipeline))
                            :resize (fn [width height]
                                      (cdq.game.resize/do! @state width height))
                            :pause (fn [])
                            :resume (fn [])}))
                        (app-config/create config))))
