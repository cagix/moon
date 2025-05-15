(ns cdq.game.default-font
  (:require [cdq.ctx :as ctx]
            [cdq.utils :as utils]
            [clojure.graphics :as graphics]))

(defn do! []
  (utils/bind-root #'ctx/default-font (graphics/truetype-font {:file "fonts/exocet/films.EXL_____.ttf"
                                                               :size 16
                                                               :quality-scaling 2})))
