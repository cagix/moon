(ns cdq.tx.stage
  (:require [cdq.graphics :as graphics]
            [cdq.stage]
            [cdq.world :as world]
            [clojure.repl]
            [com.badlogic.gdx.scenes.scene2d :as scene2d]
            [com.badlogic.gdx.scenes.scene2d.stage :as stage]
            [gdl.utils :as utils]))

(defn show-error-window!
  [{:keys [ctx/stage]} throwable]
  (stage/add! stage (scene2d/build
                     {:actor/type :actor.type/window
                      :title "Error"
                      :rows [[{:actor {:actor/type :actor.type/label
                                       :label/text (binding [*print-level* 3]
                                                     (utils/with-err-str
                                                       (clojure.repl/pst throwable)))}}]]
                      :modal? true
                      :close-button? true
                      :close-on-escape? true
                      :center? true
                      :pack? true})))

(defn player-add-skill!
  [{:keys [ctx/graphics
           ctx/stage]}
   skill]
  (cdq.stage/add-skill! stage
                        {:skill-id (:property/id skill)
                         :texture-region (graphics/texture-region graphics (:entity/image skill))
                         :tooltip-text (fn [{:keys [ctx/world]}]
                                         (world/info-text world skill))})
  nil)

(defn player-set-item! [{:keys [ctx/graphics
                                ctx/stage]}
                        cell item]
  (cdq.stage/set-item! stage cell
                       {:texture-region (graphics/texture-region graphics (:entity/image item))
                        :tooltip-text (fn [{:keys [ctx/world]}]
                                        (world/info-text world item))})
  nil)

(defn player-remove-item! [{:keys [ctx/stage]}
                           cell]
  (cdq.stage/remove-item! stage cell)
  nil)

(defn toggle-inventory-visible! [{:keys [ctx/stage]}]
  (cdq.stage/toggle-inventory-visible! stage)
  nil)

(defn show-message! [{:keys [ctx/stage]} message]
  (cdq.stage/show-text-message! stage message)
  nil)

(defn show-modal! [{:keys [ctx/stage]} opts]
  (cdq.stage/show-modal-window! stage (stage/viewport stage) opts)
  nil)
