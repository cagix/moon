(ns cdq.ctx.create.ui.hp-mana-bar
  (:require [cdq.entity.stats :as stats]
            [cdq.graphics :as graphics]
            [cdq.graphics.textures :as textures]
            [clojure.gdx.scene2d.actor :as actor]
            [cdq.ui :as ui]
            [cdq.ui.stage :as stage]
            [clojure.val-max :as val-max]
            [clojure.utils :as utils]))

(let [config {:rahmen-file "images/rahmen.png"
              :rahmenw 150
              :rahmenh 26
              :hpcontent-file "images/hp.png"
              :manacontent-file "images/mana.png"
              :y-mana 80}]
  (defn create [{:keys [ctx/stage
                        ctx/graphics]}]
    (let [{:keys [rahmen-file
                  rahmenw
                  rahmenh
                  hpcontent-file
                  manacontent-file
                  y-mana]} config
          [x y-mana] [(/ (ui/viewport-width stage) 2)
                      y-mana]
          rahmen-tex-reg (textures/texture-region graphics {:image/file rahmen-file})
          y-hp (+ y-mana rahmenh)
          render-hpmana-bar (fn [x y content-file minmaxval name]
                              [[:draw/texture-region rahmen-tex-reg [x y]]
                               [:draw/texture-region
                                (textures/texture-region graphics
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
                         (let [stats (:entity/stats @(:world/player-eid world))
                               x (- x (/ rahmenw 2))]
                           (concat
                            (render-hpmana-bar x y-hp   hpcontent-file   (stats/get-hitpoints stats) "HP")
                            (render-hpmana-bar x y-mana manacontent-file (stats/get-mana      stats) "MP"))))]
      (actor/create
       {:act (fn [_this _delta])
        :draw (fn [actor _batch _parent-alpha]
                (when-let [stage (actor/stage actor)]
                  (graphics/draw! (:ctx/graphics (stage/ctx stage))
                                  (create-draws (stage/ctx stage)))))}))))
