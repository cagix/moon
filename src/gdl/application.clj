(ns gdl.application
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [com.badlogic.gdx :as gdx]
            [com.badlogic.gdx.backends.lwjgl3 :as lwjgl])
  (:import (com.badlogic.gdx ApplicationListener))
  (:gen-class))

(defprotocol Listener
  (create [_ context])
  (dispose [_])
  (render [_])
  (resize [_ width height])
  (pause [_])
  (resume [_]))

(defn start! [{:keys [listener config]}]
  (lwjgl/application (reify ApplicationListener
                       (create [_]
                         (create listener {:ctx/app      (gdx/app)
                                           :ctx/audio    (gdx/audio)
                                           :ctx/files    (gdx/files)
                                           :ctx/graphics (gdx/graphics)
                                           :ctx/input    (gdx/input)}))
                       (dispose [_]
                         (dispose listener))
                       (render [_]
                         (render listener))
                       (resize [_ width height]
                         (resize listener width height))
                       (pause [_]
                         (pause listener))
                       (resume [_]
                         (resume listener)))
                     config))

(require 'com.badlogic.gdx.scenes.scene2d.ui.horizontal-group
         'com.badlogic.gdx.scenes.scene2d.ui.stack)

(defn -main [config-path]
  (let [{:keys [listener
                config
                state-atom-var-sym]} (-> config-path io/resource slurp edn/read-string)
        state @(requiring-resolve state-atom-var-sym)]
    (start!
     {:listener (let [{:keys [create
                              dispose
                              render
                              resize]} listener
                      create-pipeline (map requiring-resolve create)
                      dispose (requiring-resolve dispose)
                      render-pipeline (map requiring-resolve render)
                      resize (requiring-resolve resize)]
                  (reify Listener
                    (create [_ context]
                      (reset! state (reduce (fn [ctx f]
                                              (f ctx))
                                            context
                                            create-pipeline)))
                    (dispose [_]
                      (dispose @state))
                    (pause [_])
                    (render [_]
                      (swap! state (fn [ctx]
                                     (reduce (fn [ctx f]
                                               (f ctx))
                                             ctx
                                             render-pipeline))))
                    (resize [_ width height]
                      (resize @state width height))
                    (resume [_])))
      :config config})))
