(ns anvil.widgets.hp-mana-bar
  (:require [anvil.entity :as entity]
            [anvil.widgets :as widgets]
            [anvil.world :as world]
            [gdl.context :as ctx]
            [gdl.graphics :as g]
            [gdl.ui :refer [ui-actor]]
            [gdl.val-max :as val-max]))

(defn- render-infostr-on-bar [infostr x y h]
  (g/draw-text {:text infostr
                :x (+ x 75)
                :y (+ y 2)
                :up? true}))

(defn-impl widgets/hp-mana-bar []
  (let [rahmen      (ctx/sprite "images/rahmen.png")
        hpcontent   (ctx/sprite "images/hp.png")
        manacontent (ctx/sprite "images/mana.png")
        x (/ g/viewport-width 2)
        [rahmenw rahmenh] (:pixel-dimensions rahmen)
        y-mana 80 ; action-bar-icon-size
        y-hp (+ y-mana rahmenh)
        render-hpmana-bar (fn [x y contentimage minmaxval name]
                            (g/draw-image rahmen [x y])
                            (g/draw-image (ctx/sub-sprite contentimage
                                                          [0 0 (* rahmenw (val-max/ratio minmaxval)) rahmenh])
                                          [x y])
                            (render-infostr-on-bar (str (readable-number (minmaxval 0)) "/" (minmaxval 1) " " name) x y rahmenh))]
    (ui-actor {:draw (fn []
                       (let [player-entity @world/player-eid
                             x (- x (/ rahmenw 2))]
                         (render-hpmana-bar x y-hp   hpcontent   (entity/hitpoints   player-entity) "HP")
                         (render-hpmana-bar x y-mana manacontent (entity/mana player-entity) "MP")))})))
