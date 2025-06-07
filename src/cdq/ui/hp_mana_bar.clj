(ns cdq.ui.hp-mana-bar
  (:require [cdq.entity :as entity]
            [cdq.val-max :as val-max]
            [cdq.utils :as utils]
            [gdl.graphics :as g]
            [gdl.ui :as ui]))

(defn- render-infostr-on-bar [infostr x y h]
  [:draw/text {:text infostr
               :x (+ x 75)
               :y (+ y 2)
               :up? true}])

(defn create [{:keys [ctx/graphics]}
              {:keys [rahmen
                      hpcontent
                      manacontent
                      y-mana
                      ]}
              ]
  (let [[x y-mana] [(/ (:width (:ui-viewport graphics)) 2)
                    y-mana]
        rahmen      (g/sprite graphics rahmen)
        hpcontent   (g/sprite graphics hpcontent)
        manacontent (g/sprite graphics manacontent)
        [rahmenw rahmenh] (:sprite/pixel-dimensions rahmen)
        y-hp (+ y-mana rahmenh)
        render-hpmana-bar (fn [graphics x y contentimage minmaxval name]
                            [[:draw/image rahmen [x y]]
                             [:draw/image
                              (g/sub-sprite graphics
                                            contentimage
                                            [0 0 (* rahmenw (val-max/ratio minmaxval)) rahmenh])
                              [x y]]
                             (render-infostr-on-bar (str (utils/readable-number (minmaxval 0))
                                                         "/"
                                                         (minmaxval 1)
                                                         " "
                                                         name)
                                                    x
                                                    y
                                                    rahmenh)])
        create-draws (fn [{:keys [ctx/graphics
                                  ctx/player-eid]}]
                       (let [player-entity @player-eid
                             x (- x (/ rahmenw 2))]
                         (concat
                          (render-hpmana-bar graphics x y-hp   hpcontent   (entity/hitpoints player-entity) "HP")
                          (render-hpmana-bar graphics x y-mana manacontent (entity/mana      player-entity) "MP"))))]
    (ui/actor
     {:draw (fn [_this {:keys [ctx/graphics] :as ctx}]
              (g/handle-draws! graphics (create-draws ctx)))})))
