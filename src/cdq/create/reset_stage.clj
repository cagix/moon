(ns cdq.create.reset-stage
  (:require cdq.create.ui.dev-menu
            cdq.create.ui.action-bar
            cdq.create.ui.hp-mana-bar
            cdq.create.ui.windows
            cdq.create.ui.entity-info
            cdq.create.ui.inventory
            cdq.create.ui.player-state-draw
            cdq.create.ui.message
            [clojure.scene2d :as scene2d]
            [clojure.scene2d.stage :as stage]))

(def ^:private ui-actors
  [[cdq.create.ui.dev-menu/create]
   [cdq.create.ui.action-bar/create]
   [cdq.create.ui.hp-mana-bar/create {:rahmen-file "images/rahmen.png"
                                      :rahmenw 150
                                      :rahmenh 26
                                      :hpcontent-file "images/hp.png"
                                      :manacontent-file "images/mana.png"
                                      :y-mana 80}]
   [cdq.create.ui.windows/create [cdq.create.ui.entity-info/create
                                  cdq.create.ui.inventory/create]]
   [cdq.create.ui.player-state-draw/create]
   [cdq.create.ui.message/create {:duration-seconds 0.5
                                  :name "player-message"}]])

(defn do!
  [{:keys [ctx/stage]
    :as ctx}]
  (stage/clear! stage)
  (let [actors (map #(let [[f & params] %]
                       (apply f ctx params))
                    ui-actors)]
    (doseq [actor actors]
      (stage/add! stage (scene2d/build actor))))
  ctx)
