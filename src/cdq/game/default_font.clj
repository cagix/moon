(ns cdq.game.default-font
  (:require [cdq.ctx :as ctx]
            [cdq.utils :as utils]
            [clojure.graphics :as graphics]))

(defn do! []
  (utils/bind-root #'ctx/default-font (graphics/truetype-font ctx/font-config)))
