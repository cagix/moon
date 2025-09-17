(ns cdq.ui.hp-mana-bar
  (:require [cdq.graphics :as graphics]
            [clojure.utils :as utils]
            [cdq.val-max :as val-max]
            [cdq.stage :as stage]
            [cdq.stats :as modifiers]))

(defn create
  [{:keys [ctx/graphics
           ctx/stage]}
   {:keys [rahmen-file
           rahmenw
           rahmenh
           hpcontent-file
           manacontent-file
           y-mana]}]
  (let [[x y-mana] [(/ (stage/viewport-width stage) 2)
                    y-mana]
        rahmen-tex-reg (graphics/texture-region graphics {:image/file rahmen-file})
        y-hp (+ y-mana rahmenh)
        render-hpmana-bar (fn [x y content-file minmaxval name]
                            [[:draw/texture-region rahmen-tex-reg [x y]]
                             [:draw/texture-region
                              (graphics/texture-region graphics
                                                       {:image/file content-file
                                                        :image/bounds [0 0 (* rahmenw (val-max/ratio minmaxval)) rahmenh]})
                              [x y]]
                             [:draw/text {:text (str (utils/readable-number (minmaxval 0))
                                                     "/"
                                                     (minmaxval 1)
                                                     " "
                                                     name)
                                          :x (+ x 75)
                                          :y (+ y 2)
                                          :up? true}]])
        create-draws (fn [{:keys [ctx/world]}]
                       (let [stats (:creature/stats @(:world/player-eid world))
                             x (- x (/ rahmenw 2))]
                         (concat
                          (render-hpmana-bar x y-hp   hpcontent-file   (modifiers/get-hitpoints stats) "HP")
                          (render-hpmana-bar x y-mana manacontent-file (modifiers/get-mana      stats) "MP"))))]
    {:actor/type :actor.type/actor
     :draw (fn [_this ctx]
             (create-draws ctx))}))
