(ns cdq.ui.hp-mana-bar
  (:require [cdq.image :as image]
            [cdq.utils :as utils]
            [cdq.val-max :as val-max]
            [cdq.stats :as modifiers]))

(defn create
  [{:keys [ctx/textures
           ctx/ui-viewport]}
   {:keys [rahmen-file
           rahmenw
           rahmenh
           hpcontent-file
           manacontent-file
           y-mana]}]
  (let [[x y-mana] [(/ (:viewport/width ui-viewport) 2)
                    y-mana]
        rahmen-tex-reg (image/texture-region {:image/file rahmen-file} textures)
        y-hp (+ y-mana rahmenh)
        render-hpmana-bar (fn [x y content-file minmaxval name]
                            [[:draw/texture-region rahmen-tex-reg [x y]]
                             [:draw/texture-region
                              (image/texture-region {:image/file content-file
                                                     :image/bounds [0 0 (* rahmenw (val-max/ratio minmaxval)) rahmenh]}
                                                    textures)
                              [x y]]
                             [:draw/text {:text (str (utils/readable-number (minmaxval 0))
                                                     "/"
                                                     (minmaxval 1)
                                                     " "
                                                     name)
                                          :x (+ x 75)
                                          :y (+ y 2)
                                          :up? true}]])
        create-draws (fn [{:keys [ctx/player-eid]}]
                       (let [stats (:creature/stats @player-eid)
                             x (- x (/ rahmenw 2))]
                         (concat
                          (render-hpmana-bar x y-hp   hpcontent-file   (modifiers/get-hitpoints stats) "HP")
                          (render-hpmana-bar x y-mana manacontent-file (modifiers/get-mana      stats) "MP"))))]
    {:actor/type :actor.type/actor
     :draw (fn [_this ctx]
             (create-draws ctx))}))
