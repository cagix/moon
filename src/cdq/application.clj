(ns cdq.application
  (:require [clojure.edn :as edn]
            [clojure.gdx.application :as application]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.gdx.utils :as utils]
            [clojure.gdx.utils.viewport :as viewport]
            [clojure.java.awt.taskbar :as taskbar]
            [clojure.java.io :as io]
            [clojure.lwjgl.system.configuration :as configuration]))

(def state (atom nil))

(defn -main []
  (let [config (-> "cdq.application.edn"
                   io/resource
                   slurp
                   edn/read-string)]
    (taskbar/set-icon (:taskbar-icon config))
    (when (= (utils/operating-system) :mac)
      (configuration/set-glfw-library-name "glfw_async"))
    (lwjgl/application (reify application/Listener
                         (create [_]
                           (reset! state (reduce (fn [context [k ns-sym]]
                                                   (require ns-sym)
                                                   (let [f (resolve (symbol (str ns-sym "/create")))]
                                                     (assoc context k (f context))))
                                                 {}
                                                 (:create-components config))))

                         (dispose [_]
                           (doseq [[k value] @state
                                   :when (utils/disposable? value)]
                             (utils/dispose value)))

                         (pause [_])

                         (render [_]
                           (swap! state (fn [context]
                                          (reduce (fn [context f]
                                                    (f context))
                                                  context
                                                  (for [ns-sym (:render-fns config)]
                                                    (do
                                                     (require ns-sym)
                                                     (resolve (symbol (str ns-sym "/render")))))))))

                         (resize [_ width height]
                           (let [context @state]
                             (viewport/update (:cdq.graphics/ui-viewport    context) width height :center-camera? true)
                             (viewport/update (:cdq.graphics/world-viewport context) width height)))

                         (resume [_]))
                       (:lwjgl-app config))))
