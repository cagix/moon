(ns cdq.application
  (:require cdq.info-impl
            [cdq.audio :as audio]
            [cdq.graphics :as graphics]
            [cdq.world :as world]
            [clojure.scene2d.vis-ui :as vis-ui]
            [cdq.graphics.ui-viewport :as ui-viewport]
            [cdq.graphics.world-viewport :as world-viewport]
            clojure.scene2d.builds
            cdq.scene2d.build.editor-overview-window
            cdq.scene2d.build.editor-window
            cdq.scene2d.build.map-widget-table
            clojure.scene2d.build.actor
            clojure.scene2d.build.group
            clojure.scene2d.build.horizontal-group
            clojure.scene2d.build.scroll-pane
            clojure.scene2d.build.separator-horizontal
            clojure.scene2d.build.separator-vertical
            clojure.scene2d.build.stack
            clojure.scene2d.build.widget
            cdq.ui.actor-information
            cdq.ui.error-window
            [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:import (com.badlogic.gdx ApplicationListener
                             Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (org.lwjgl.system Configuration))
  (:gen-class))

(def state (atom nil))

(defn pipeline [ctx pipeline]
  (reduce (fn [ctx [f & args]]
            (apply f ctx args))
          ctx
          pipeline))

(defn- resize! [{:keys [ctx/graphics]} width height]
  (ui-viewport/update!    graphics width height)
  (world-viewport/update! graphics width height))

(defn- dispose!
  [{:keys [ctx/audio
           ctx/graphics
           ctx/world]}]
  (vis-ui/dispose!)
  (audio/dispose! audio)
  (graphics/dispose! graphics)
  (world/dispose! world))

(defn -main []
  (let [app (-> "cdq.application.edn"
                io/resource
                slurp
                edn/read-string)
        req-resolve (fn [sym sym-format]
                      (requiring-resolve (symbol (format sym-format sym))))
        create-pipeline (map #(update % 0 req-resolve "cdq.ctx.create.%s/do!") (:create-pipeline app))
        render-pipeline (map #(update % 0 req-resolve "cdq.ctx.render.%s/do!") (:render-pipeline app))]
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
    (Lwjgl3Application. (reify ApplicationListener
                          (create [_]
                            (reset! state (pipeline {:ctx/gdx {:clojure.gdx/audio    Gdx/audio
                                                               :clojure.gdx/files    Gdx/files
                                                               :clojure.gdx/graphics Gdx/graphics
                                                               :clojure.gdx/input    Gdx/input}}
                                                    create-pipeline)))
                          (dispose [_]
                            (dispose! @state))
                          (render [_]
                            (swap! state pipeline render-pipeline))
                          (resize [_ width height]
                            (resize! @state width height))
                          (pause [_])
                          (resume [_]))
                        (doto (Lwjgl3ApplicationConfiguration.)
                          (.setTitle "Cyber Dungeon Quest")
                          (.setWindowedMode 1440 900)
                          (.setForegroundFPS 60)))))
