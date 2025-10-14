(ns cdq.application
  (:require cdq.ui.editor.schemas-impl
            [cdq.audio :as audio]
            [cdq.graphics :as graphics]
            [cdq.graphics.ui-viewport :as ui-viewport]
            [cdq.graphics.world-viewport :as world-viewport]
            [cdq.ui :as ui]
            [cdq.world :as world]
            [clojure.config :as config]
            [clojure.gdx :as gdx]
            [clojure.gdx.application.listener :as listener]
            [clojure.gdx.backends.lwjgl.application :as application]
            [clojure.gdx.backends.lwjgl.application.config :as app-config]
            [clojure.lwjgl.system.configuration :as lwjgl-config])
  (:gen-class))

(defn- create! []
  (reduce (fn [ctx [f & params]]
            (apply (requiring-resolve f) ctx params))
          (gdx/context)
          (config/edn-resource "create-pipeline.edn")))

(defn- dispose!
  [{:keys [ctx/audio
           ctx/graphics
           ctx/world]}]
  (audio/dispose! audio)
  (graphics/dispose! graphics)
  (ui/dispose!)
  (world/dispose! world))

(defn- render! [ctx steps]
  (reduce (fn [ctx f]
            ((requiring-resolve f) ctx))
          ctx
          steps))

(defn- resize! [{:keys [ctx/graphics]} width height]
  (ui-viewport/update!    graphics width height)
  (world-viewport/update! graphics width height))

(def state (atom nil))

(defn -main []
  (let [{:keys [config]} (config/edn-resource "config.edn")]
    (lwjgl-config/set-glfw-library-name! "glfw_async")
    (application/create (listener/create
                         (let [render-pipeline (config/edn-resource "render-pipeline.edn")]
                           {:create (fn []
                                      (reset! state (create!)))
                            :dispose (fn []
                                       (dispose! @state))
                            :render (fn []
                                      (swap! state render! render-pipeline))
                            :resize (fn [width height]
                                      (resize! @state width height))
                            :pause (fn [])
                            :resume (fn [])}))
                        (app-config/create config))))
