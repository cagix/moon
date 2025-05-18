(ns cdq.application.create.assets
  (:require [cdq.ctx :as ctx]
            [cdq.utils :refer [bind-root]]
            [gdl.assets :as assets]))

(defn do! []
  (bind-root #'ctx/assets (assets/create (:assets ctx/config))))
