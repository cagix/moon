(ns cdq.ui.actor
  (:require [cdq.ctx :as ctx]
            [cdq.ui.ctx-stage :as ctx-stage]
            [cdq.ui.tooltip :as tooltip]
            [cdq.ui.utils :as utils]
            [clojure.gdx.scenes.scene2d.actor :as actor]))

(defn set-opts! [actor opts]
  (actor/set-opts! actor opts)
  (when-let [tooltip (:tooltip opts)]
    (tooltip/add! actor tooltip))
  (when-let [f (:click-listener opts)]
    (.addListener actor (utils/click-listener f)))
  actor)

; actor was removed -> stage nil -> context nil -> error on text-buttons/etc.
(defn try-act [actor delta f]
  (when-let [ctx (when-let [stage (actor/get-stage actor)]
                   (ctx-stage/get-ctx stage))]
    (f actor delta ctx)))

(defn try-draw [actor f]
  (when-let [ctx (when-let [stage (actor/get-stage actor)]
                   (ctx-stage/get-ctx stage))]
    (ctx/handle-draws! ctx (f actor ctx))))

(defn create [opts]
  (doto (actor/create
         (fn [this delta]
           (when-let [f (:act opts)]
             (try-act this delta f)))
         (fn [this _batch _parent-alpha]
           (when-let [f (:draw opts)]
             (try-draw this f))))
    (set-opts! opts)))
