(ns moon.widgets.hp-mana
  (:require [gdl.graphics.gui-view :as gui-view]
            [gdl.ui :as ui]
            [gdl.utils :refer [readable-number]]
            [moon.core :refer [draw-text draw-image image sub-image]]
            [moon.entity.hp :as hp]
            [moon.entity.mana :as mana]
            [moon.player :as player]
            [moon.val-max :as val-max]))

(defn- render-infostr-on-bar [infostr x y h]
  (draw-text {:text infostr
              :x (+ x 75)
              :y (+ y 2)
              :up? true}))

(defn actor []
  (let [rahmen      (image "images/rahmen.png")
        hpcontent   (image "images/hp.png")
        manacontent (image "images/mana.png")
        x (/ (gui-view/width) 2)
        [rahmenw rahmenh] (:pixel-dimensions rahmen)
        y-mana 80 ; action-bar-icon-size
        y-hp (+ y-mana rahmenh)
        render-hpmana-bar (fn [x y contentimage minmaxval name]
                            (draw-image rahmen [x y])
                            (draw-image (sub-image contentimage [0 0 (* rahmenw (val-max/ratio minmaxval)) rahmenh])
                                        [x y])
                            (render-infostr-on-bar (str (readable-number (minmaxval 0)) "/" (minmaxval 1) " " name) x y rahmenh))]
    (ui/actor {:draw (fn []
                       (let [player-entity @player/eid
                             x (- x (/ rahmenw 2))]
                         (render-hpmana-bar x y-hp   hpcontent   (hp/value   player-entity) "HP")
                         (render-hpmana-bar x y-mana manacontent (mana/value player-entity) "MP")))})))
