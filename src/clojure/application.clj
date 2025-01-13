(ns clojure.application
  (:require [clojure.utils :as utils]))

(def state (atom nil))

(defn create [create-components]
  (reset! state
          (reduce (fn [context [k fn-invoc]]
                    (assoc context k (utils/req-resolve-call fn-invoc context)))
                  {}
                  create-components)))

(defn dispose []
  (doseq [[k value] @state
          :when (utils/disposable? value)]
    ;(println "Disposing " k " - " value)
    (utils/dispose value)))

; TODO runnables here ?!?!
; can make another atom
(defn post-runnable [f]
  (.postRunnable com.badlogic.gdx.Gdx/app #(f @state)))

(defn render [render-fns]
  (swap! state (fn [context]
                 (reduce (fn [context fn-sym]
                           (@(utils/req-resolve fn-sym) context))
                         context
                         render-fns))))

(defn resize [resize-fn-invocation-form width height]
  (utils/req-resolve-call resize-fn-invocation-form @state width height))
