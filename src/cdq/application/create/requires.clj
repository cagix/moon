(ns cdq.application.create.requires
  (:require [cdq.ctx :as ctx]))

(defn do! []
  (run! require (:requires ctx/config)))
