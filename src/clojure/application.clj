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

(def ^:private runnables (atom []))

(defn post-runnable [f]
  (swap! runnables conj f))

(defn render [render-fns]
  (when (seq @runnables)
    (println "Execute " (count @runnables) "runnables.")
    (swap! state (fn [context]
                   (reduce (fn [context f] (f context))
                           context
                           @runnables)))
    (reset! runnables []))
  (swap! state (fn [context]
                 (reduce (fn [context fn-invoc]
                           (utils/req-resolve-call fn-invoc context))
                         context
                         render-fns))))

(defn resize [resize-fn-invocation-form width height]
  (utils/req-resolve-call resize-fn-invocation-form @state width height))
