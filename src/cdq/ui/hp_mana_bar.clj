(ns cdq.ui.hp-mana-bar
  (:require [cdq.world.entity :as entity]
            [cdq.val-max :as val-max]
            [cdq.utils :as utils]
            [cdq.ctx.graphics :as g]))

(defn create
  [{:keys [ctx/graphics]}
   {:keys [rahmen-file
           rahmenw
           rahmenh
           hpcontent-file
           manacontent-file
           y-mana]}]
  (let [[x y-mana] [(/ (:viewport/width (:ui-viewport graphics)) 2)
                    y-mana]
        rahmen-tex-reg (g/image->texture-region graphics {:image/file rahmen-file})
        y-hp (+ y-mana rahmenh)
        render-hpmana-bar (fn [graphics x y content-file minmaxval name]
                            [[:draw/texture-region rahmen-tex-reg [x y]]
                             [:draw/texture-region
                              (g/image->texture-region graphics
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
        create-draws (fn [{:keys [ctx/graphics
                                  ctx/world]}]
                       (let [player-entity @(:world/player-eid world)
                             x (- x (/ rahmenw 2))]
                         (concat
                          (render-hpmana-bar graphics x y-hp   hpcontent-file   (entity/hitpoints player-entity) "HP")
                          (render-hpmana-bar graphics x y-mana manacontent-file (entity/mana      player-entity) "MP"))))]
    {:actor/type :actor.type/actor
     :draw (fn [_this {:keys [ctx/graphics] :as ctx}]
             (g/handle-draws! graphics (create-draws ctx)))}))
