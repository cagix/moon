(ns cdq.ui.hp-mana-bar
  (:require [gdl.context :as c]
            [gdl.ui :refer [ui-actor]]
            [gdl.utils :refer [readable-number]]
            [cdq.entity :as entity]
            [cdq.val-max :as val-max]))

(defn- render-infostr-on-bar [c infostr x y h]
  (c/draw-text c
               {:text infostr
                :x (+ x 75)
                :y (+ y 2)
                :up? true}))

(defn create [{:keys [gdl.graphics/ui-viewport] :as c} _config]
  (let [rahmen      (c/sprite c "images/rahmen.png")
        hpcontent   (c/sprite c "images/hp.png")
        manacontent (c/sprite c "images/mana.png")
        x (/ (:width ui-viewport) 2)
        [rahmenw rahmenh] (:pixel-dimensions rahmen)
        y-mana 80 ; action-bar-icon-size
        y-hp (+ y-mana rahmenh)
        render-hpmana-bar (fn [c x y contentimage minmaxval name]
                            (c/draw-image c rahmen [x y])
                            (c/draw-image c
                                          (c/sub-sprite c
                                                        contentimage
                                                        [0 0 (* rahmenw (val-max/ratio minmaxval)) rahmenh])
                                          [x y])
                            (render-infostr-on-bar c (str (readable-number (minmaxval 0)) "/" (minmaxval 1) " " name) x y rahmenh))]
    (ui-actor {:draw (fn [{:keys [cdq.context/player-eid] :as c}]
                       (let [player-entity @player-eid
                             x (- x (/ rahmenw 2))]
                         (render-hpmana-bar c x y-hp   hpcontent   (entity/hitpoints   player-entity) "HP")
                         (render-hpmana-bar c x y-mana manacontent (entity/mana        player-entity) "MP")))})))
