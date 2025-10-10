(ns cdq.application
  (:require [clojure.gdx.backends.lwjgl.application :as application]))

(def state (atom nil))

(defn create
  [{:keys [listener config]}]
  (application/create (let [[f & params] listener]
                        (apply (requiring-resolve f) params))
                      (let [[f & params] config]
                        (apply (requiring-resolve f) params))))
