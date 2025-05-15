(ns cdq.game.load-assets
  (:require [cdq.ctx :as ctx]
            [cdq.utils :as utils]
            [clojure.assets :as assets]))

(defn do! []
  (utils/bind-root #'ctx/assets (assets/create)))
