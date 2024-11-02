(ns moon.widgets.hp-mana
  (:require [gdl.graphics.gui-view :as gui-view]
            [gdl.graphics.image :as img]
            [gdl.graphics.text :as text]
            [gdl.ui :as ui]
            [gdl.utils :refer [readable-number]]
            [moon.component :as component]
            [moon.entity :as entity]
            [moon.player :as player]
            [moon.val-max :as val-max]))

(defn- render-infostr-on-bar [infostr x y h]
  (text/draw {:text infostr
              :x (+ x 75)
              :y (+ y 2)
              :up? true}))

(defmethods :widgets/hp-mana
  (component/create [_]
    (let [rahmen      (img/image "images/rahmen.png")
          hpcontent   (img/image "images/hp.png")
          manacontent (img/image "images/mana.png")
          x (/ (gui-view/width) 2)
          [rahmenw rahmenh] (:pixel-dimensions rahmen)
          y-mana 80 ; action-bar-icon-size
          y-hp (+ y-mana rahmenh)
          render-hpmana-bar (fn [x y contentimg minmaxval name]
                              (img/draw rahmen [x y])
                              (img/draw (img/sub-image contentimg [0 0 (* rahmenw (val-max/ratio minmaxval)) rahmenh])
                                        [x y])
                              (render-infostr-on-bar (str (readable-number (minmaxval 0)) "/" (minmaxval 1) " " name) x y rahmenh))]
      (ui/actor {:draw (fn []
                         (let [player-entity @player/eid
                               x (- x (/ rahmenw 2))]
                           (render-hpmana-bar x y-hp   hpcontent   (entity/stat player-entity :stats/hp) "HP")
                           (render-hpmana-bar x y-mana manacontent (entity/stat player-entity :stats/mana) "MP")))}))))
