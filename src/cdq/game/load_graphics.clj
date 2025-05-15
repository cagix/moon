(ns cdq.game.load-graphics
  (:require [cdq.ctx :as ctx]
            [cdq.impl.graphics :as graphics]
            [cdq.utils :as utils]))

(defn do! []
  (utils/bind-root #'ctx/graphics (graphics/create {:tile-size 48
                                                    :world-viewport {:width 1440 :height 900}
                                                    :ui-viewport {:width 1440 :height 900}})))
