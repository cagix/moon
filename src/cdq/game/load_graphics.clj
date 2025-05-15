(ns cdq.game.load-graphics
  (:require [cdq.ctx :as ctx]
            [cdq.impl.graphics :as graphics]
            [cdq.utils :as utils]))

(defn do! []
  (utils/bind-root #'ctx/graphics (graphics/create {:default-font {:file "fonts/exocet/films.EXL_____.ttf"
                                                                   :size 16
                                                                   :quality-scaling 2}
                                                    :tile-size 48
                                                    :world-viewport {:width 1440 :height 900}
                                                    :ui-viewport {:width 1440 :height 900}})))
