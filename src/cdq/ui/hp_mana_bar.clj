(ns cdq.ui.hp-mana-bar
  (:require [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.graphics :as graphics]
            [cdq.utils :as utils]
            [cdq.val-max :as val-max]
            [gdl.ui :as ui]))

(defn- render-infostr-on-bar [infostr x y h]
  [:draw/text {:text infostr
               :x (+ x 75)
               :y (+ y 2)
               :up? true}])

(defn create [[x y-mana]
              {:keys [ctx/world-unit-scale]
               :as ctx}]
  (let [rahmen      (graphics/sprite (g/texture ctx "images/rahmen.png") world-unit-scale)
        hpcontent   (graphics/sprite (g/texture ctx "images/hp.png")     world-unit-scale)
        manacontent (graphics/sprite (g/texture ctx "images/mana.png")   world-unit-scale)
        [rahmenw rahmenh] (:pixel-dimensions rahmen)
        y-hp (+ y-mana rahmenh)
        render-hpmana-bar (fn [ctx x y contentimage minmaxval name]
                            [[:draw/image rahmen [x y]]
                             [:draw/image
                              (graphics/sub-sprite contentimage
                                                   [0
                                                    0
                                                    (* rahmenw (val-max/ratio minmaxval))
                                                    rahmenh]
                                                   world-unit-scale)
                              [x y]]
                             (render-infostr-on-bar (str (utils/readable-number (minmaxval 0))
                                                         "/"
                                                         (minmaxval 1)
                                                         " "
                                                         name)
                                                    x
                                                    y
                                                    rahmenh)])
        create-draws (fn [{:keys [ctx/player-eid] :as ctx}]
                       (let [player-entity @player-eid
                             x (- x (/ rahmenw 2))]
                         (concat
                          (render-hpmana-bar ctx x y-hp   hpcontent   (entity/hitpoints player-entity) "HP")
                          (render-hpmana-bar ctx x y-mana manacontent (entity/mana      player-entity) "MP"))))]
    (ui/actor
     {:draw (fn [_this ctx]
              (g/handle-draws! ctx (create-draws ctx)))})))
