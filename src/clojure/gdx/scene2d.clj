(ns clojure.gdx.scene2d
  (:require [clojure.gdx.scene2d.actor :as actor]
            [clojure.gdx.scene2d.actor.decl :as actor.decl])
  (:import (clojure.gdx.scene2d Stage)
           (com.badlogic.gdx.scenes.scene2d Actor
                                            Group)))

(defn group [opts]
  (doto (Group.)
    (actor.decl/set-group-opts! opts)))

(defn stage [viewport batch]
  (Stage. viewport batch (atom nil)))

; actor was removed -> stage nil -> context nil -> error on text-buttons/etc.
(defn try-act [actor delta f]
  (when-let [ctx (when-let [stage (actor/get-stage actor)]
                   @(.ctx ^clojure.gdx.scene2d.Stage stage))]
    (f actor delta ctx)))

(defprotocol Context
  (handle-draws! [_ draws]))

(defn try-draw [actor f]
  (when-let [ctx (when-let [stage (actor/get-stage actor)]
                   @(.ctx ^clojure.gdx.scene2d.Stage stage))]
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
    (actor.decl/set-actor-opts! opts)))

(defmethod actor.decl/build :actor.type/actor [opts] (actor opts))
(defmethod actor.decl/build :actor.type/group [opts] (group opts))
