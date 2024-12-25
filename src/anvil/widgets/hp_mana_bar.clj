(ns anvil.widgets.hp-mana-bar
  (:require [anvil.entity :as entity]
            [anvil.widgets :as widgets]
            [anvil.world :as world]
            [gdl.context :as c]
            [gdl.ui :refer [ui-actor]]
            [gdl.val-max :as val-max]))

(defn- render-infostr-on-bar [c infostr x y h]
  (c/draw-text c
               {:text infostr
                :x (+ x 75)
                :y (+ y 2)
                :up? true}))

(defn-impl widgets/hp-mana-bar [{:keys [gdl.context/viewport] :as c}]
  (let [rahmen      (c/sprite c "images/rahmen.png")
        hpcontent   (c/sprite c "images/hp.png")
        manacontent (c/sprite c "images/mana.png")
        x (/ (:width viewport) 2)
        [rahmenw rahmenh] (:pixel-dimensions rahmen)
        y-mana 80 ; action-bar-icon-size
        y-hp (+ y-mana rahmenh)
        render-hpmana-bar (fn [x y contentimage minmaxval name]
                            (c/draw-image c rahmen [x y])
                            (c/draw-image c
                                          (c/sub-sprite c
                                                        contentimage
                                                        [0 0 (* rahmenw (val-max/ratio minmaxval)) rahmenh])
                                          [x y])
                            (render-infostr-on-bar c (str (readable-number (minmaxval 0)) "/" (minmaxval 1) " " name) x y rahmenh))]
    (ui-actor {:draw (fn []
                       (let [player-entity @world/player-eid
                             x (- x (/ rahmenw 2))]
                         (render-hpmana-bar x y-hp   hpcontent   (entity/hitpoints   player-entity) "HP")
                         (render-hpmana-bar x y-mana manacontent (entity/mana player-entity) "MP")))})))
