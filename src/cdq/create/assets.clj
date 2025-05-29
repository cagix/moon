(ns cdq.create.assets
  (:require [gdl.assets :as assets]))

(defn do! [{:keys [ctx/files
                   ctx/config]
            :as ctx}]
  (assoc ctx :ctx/assets (assets/create files (:assets config))))
