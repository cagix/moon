(ns cdq.application.create.assets
  (:require [cdq.assets :as assets]
            [cdq.ctx :as ctx]
            [cdq.utils :refer [bind-root]]))

(defn do! []
  (bind-root #'ctx/assets (assets/create (:assets ctx/config))))
