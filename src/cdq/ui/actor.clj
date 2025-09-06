(ns cdq.ui.actor
  (:require [clojure.vis-ui.tooltip :as tooltip]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.stage :as stage]))

(defn set-opts! [actor opts]
  (actor/set-opts! actor opts)
  (when-let [tooltip (:tooltip opts)]
    (tooltip/add! actor tooltip))
  actor)

; actor was removed -> stage nil -> context nil -> error on text-buttons/etc.
(defn try-act [actor delta f]
  (when-let [ctx (when-let [stage (actor/get-stage actor)]
                   (stage/get-ctx stage))]
    (f actor delta ctx)))

(defprotocol Context
  (handle-draws! [_ draws]))

(defn try-draw [actor f]
  (when-let [ctx (when-let [stage (actor/get-stage actor)]
                   (stage/get-ctx stage))]
    (handle-draws! ctx (f actor ctx))))

(defn create [opts]
  (doto (proxy [com.badlogic.gdx.scenes.scene2d.Actor] []
          (act [delta]
            (when-let [f (:act opts)]
              (try-act this delta f)))
          (draw [_batch _parent-alpha]
            (when-let [f (:draw opts)]
              (try-draw this f))))
    (set-opts! opts)))
