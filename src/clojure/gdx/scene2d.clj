(ns clojure.gdx.scene2d
  (:require [clojure.scene2d :as scene2d]
            [clojure.gdx.scene2d.actor :as actor]
            [clojure.gdx.scene2d.group :as group]
            [clojure.gdx.scene2d.ctx-stage :as ctx-stage])
  (:import (com.badlogic.gdx.scenes.scene2d Actor
                                            Group)))

; actor was removed -> stage nil -> context nil -> error on text-buttons/etc.
(defn try-act [actor delta f]
  (when-let [ctx (when-let [stage (actor/get-stage actor)]
                   (ctx-stage/get-ctx stage))]
    (f actor delta ctx)))

(defprotocol Context
  (handle-draws! [_ draws]))

(defn try-draw [actor f]
  (when-let [ctx (when-let [stage (actor/get-stage actor)]
                   (ctx-stage/get-ctx stage))]
    (handle-draws! ctx (f actor ctx))))

; TODO have to call proxy super (fixes tooltips in pure scene2d?)
(defn actor [opts]
  (doto (proxy [Actor] []
          (act [delta]
            (when-let [f (:act opts)]
              (try-act this delta f)))
          (draw [_batch _parent-alpha]
            (when-let [f (:draw opts)]
              (try-draw this f))))
    (actor/set-opts! opts)))

(defmethod scene2d/build :actor.type/actor [opts] (actor opts))

(defn set-group-opts! [group opts]
  (run! (fn [actor-or-decl]
          ; remove if
          ; inventory widget/image
          ; inventory itself widget/window
          ; cdq.ui.windows entity info window
          (group/add! group (if (instance? Actor actor-or-decl)
                              actor-or-decl
                              (scene2d/build actor-or-decl))))
        (:group/actors opts))
  (actor/set-opts! group opts))

(defn group [opts]
  (doto (Group.)
    (set-group-opts! opts)))

(defmethod scene2d/build :actor.type/group [opts] (group opts))
