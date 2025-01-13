(ns clojure.application
  (:require [clojure.utils :as utils]))

(def state (atom nil))

(defn create [create-fns]
  (reset! state
          (reduce (fn [context [k component]]
                    (let [f (if (vector? component)
                              (component 0)
                              component)
                          params (if (and (vector? component) (= (count component) 2))
                                   (component 1)
                                   nil)]
                      (assoc context k (f context params))))
                  {}
                  @(utils/req-resolve create-fns))))

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
                 (reduce (fn [context f] (f context))
                         context
                         @(utils/req-resolve render-fns)))))

(defn resize [resize-fn-invocation-form width height]
  (utils/req-resolve-call resize-fn-invocation-form @state width height))
