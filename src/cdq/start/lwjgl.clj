(ns cdq.start.lwjgl
  (:require [clojure.gdx.backends.lwjgl :as lwjgl]))

(defn do!
  [{:keys [ctx/application-state]
    :as ctx}
   {:keys [listener
           config]}]
  (reset! application-state ctx)
  (lwjgl/start-application! (let [[f params] listener]
                              (f params))
                            config))
