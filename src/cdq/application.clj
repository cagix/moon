(ns cdq.application
  (:require [clojure.gdx.application :as application]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.gdx.utils :as utils]
            [clojure.gdx.utils.viewport :as viewport]))

(def state (atom nil))

(defn start [config]
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
                     (:lwjgl-app config)))
