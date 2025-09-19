(ns com.badlogic.gdx.scenes.scene2d.actor
  (:require [gdl.scene2d :as scene2d]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.ctx :as ctx]
            [gdl.scene2d.group :as group]
            [gdl.scene2d.stage :as stage]
            [gdl.scene2d.ui.table :as table]
            [gdl.scene2d.ui.widget :as widget]
            [gdl.scene2d.ui.window :as window]
            [com.badlogic.gdx.math.vector2 :as vector2]
            [com.badlogic.gdx.scenes.scene2d.ui.cell :as cell]
            [com.badlogic.gdx.scenes.scene2d.touchable :as touchable])
  (:import (com.badlogic.gdx.scenes.scene2d Actor
                                            Group)
           (com.badlogic.gdx.scenes.scene2d.ui HorizontalGroup
                                               Stack
                                               Table
                                               Widget
                                               WidgetGroup
                                               Window)))

; TODO make a system for _all_ scene2d/build
; also vis-ui ones?
; => then I know all widget types! editor etc. actiomnar whatever

(def ^:private opts-fn-map
  {:actor/name (fn [a v] (actor/set-name! a v))
   :actor/user-object (fn [a v] (actor/set-user-object! a v))
   :actor/visible?  (fn [a v] (actor/set-visible! a v))
   :actor/touchable (fn [a v] (actor/set-touchable! a v))
   :actor/listener (fn [a v] (actor/add-listener! a v))
   :actor/position (fn [actor [x y]]
                     (actor/set-position! actor x y))
   :actor/center-position (fn [actor [x y]]
                            (actor/set-position! actor
                                                 (- x (/ (actor/get-width  actor) 2))
                                                 (- y (/ (actor/get-height actor) 2))))})

(extend-type Actor
  gdl.scene2d.actor/Actor
  (get-stage [actor]
    (.getStage actor))

  (get-x [actor]
    (.getX actor))

  (get-y [actor]
    (.getY actor))

  (get-name [actor]
    (.getName actor))

  (user-object [actor]
    (.getUserObject actor))

  (set-user-object! [actor object]
    (.setUserObject actor object))

  (visible? [actor]
    (.isVisible actor))

  (set-visible! [actor visible?]
    (.setVisible actor visible?))

  (set-touchable! [actor touchable]
    (.setTouchable actor (touchable/k->value touchable)))

  (remove! [actor]
    (.remove actor))

  (parent [actor]
    (.getParent actor))

  (stage->local-coordinates [actor position]
    (-> actor
        (.stageToLocalCoordinates (vector2/->java position))
        vector2/->clj))

  (hit [actor [x y]]
    (.hit actor x y true))

  (set-name! [actor name]
    (.setName actor name))

  (set-position! [actor x y]
    (.setPosition actor x y))

  (get-width [actor]
    (.getWidth actor))

  (get-height [actor]
    (.getHeight actor))

  (add-listener! [actor listener]
    (.addListener actor listener))

  (set-opts! [actor opts]
    (doseq [[k v] opts
            :let [f (get opts-fn-map k)]
            :when f]
      (f actor v))
    actor))

(defn- create*
  [{:keys [actor/act
           actor/draw]
    :as opts}]
  (doto (proxy [Actor] []
          (act [delta] ; TODO call proxy super required ?-> fixes tooltips in pure scene2d? maybe also other ones..
            (act this delta))
          (draw [batch parent-alpha]
            (draw this batch parent-alpha)))
    (actor/set-opts! opts)))

(defn- get-ctx [actor]
  (when-let [stage (actor/get-stage actor)]
    (stage/get-ctx stage)))

(defn- try-act [actor delta f]
  (when-let [ctx (get-ctx actor)]
    (f actor delta ctx)))

(defn- try-draw [actor f]
  (when-let [ctx (get-ctx actor)]
    (ctx/draw! ctx (f actor ctx))))

(defmethod scene2d/build :actor.type/actor
  [{:keys [act draw]
    :as opts}]
  (create*
   (assoc opts
          :actor/act (fn [actor delta]
                       (when act
                         (try-act actor delta act)))
          :actor/draw (fn [actor _batch _parent-alpha]
                        (when draw
                          (try-draw actor draw))))))

(defmethod scene2d/build :actor.type/widget
  [opts]
  (proxy [Widget] []
    (draw [_batch _parent-alpha]
      (when-let [f (:draw opts)]
        (try-draw this f)))))

(extend-type Widget
  gdl.scene2d.ui.widget/Widget
  (set-opts! [actor opts]
    (actor/set-opts! actor opts)))

(extend-type Group
  gdl.scene2d.group/Group
  (add! [group actor]
    (.addActor group actor))

  (find-actor [group name]
    (.findActor group name))

  (clear-children! [group]
    (.clearChildren group))

  (children [group]
    (.getChildren group)))

(defn- group-opts! [group opts]
  (run! (fn [actor-or-decl]
          (group/add! group (if (instance? Actor actor-or-decl)
                              actor-or-decl
                              (scene2d/build actor-or-decl))))
        (:group/actors opts))
  (actor/set-opts! group opts))

(defmethod scene2d/build :actor.type/group [opts]
  (doto (Group.)
    (group-opts! opts)))

(defn- set-widget-group-opts!
  [^WidgetGroup widget-group
   {:keys [fill-parent? pack?]
    :as opts}]
  (.setFillParent widget-group (boolean fill-parent?)) ; <- actor? TODO
  (when pack?
    (.pack widget-group))
  (group-opts! widget-group opts))

(comment
 ; fill parent & pack is from Widget TODO ( not widget-group ?)
 com.badlogic.gdx.scenes.scene2d.ui.Widget
 ; about .pack :
 ; Generally this method should not be called in an actor's constructor because it calls Layout.layout(), which means a subclass would have layout() called before the subclass' constructor. Instead, in constructors simply set the actor's size to Layout.getPrefWidth() and Layout.getPrefHeight(). This allows the actor to have a size at construction time for more convenient use with groups that do not layout their children.
 )

(defn- horizontal-group [{:keys [space pad] :as opts}]
  (let [group (HorizontalGroup.)]
    (when space (.space group (float space)))
    (when pad   (.pad   group (float pad)))
    (group-opts! group opts)))

(defmethod scene2d/build :actor.type/horizontal-group [opts]
  (horizontal-group opts))

(defn- stack [opts]
  (doto (Stack.)
    (set-widget-group-opts! opts)))

(defmethod scene2d/build :actor.type/stack [opts]
  (stack opts))

(defn- build? [actor-or-decl]
  (try (cond
        (instance? Actor actor-or-decl)
        actor-or-decl
        (nil? actor-or-decl)
        nil
        :else
        (scene2d/build actor-or-decl))
       (catch Throwable t
         (throw (ex-info ""
                         {:actor-or-decl actor-or-decl}
                         t)))))

(extend-type Table
  gdl.scene2d.ui.table/Table
  (add! [table actor-or-decl]
    (.add table ^Actor (build? actor-or-decl)))

  (cells [table]
    (.getCells table))

  (add-rows! [table rows]
    (doseq [row rows]
      (doseq [props-or-actor row]
        (cond
         (map? props-or-actor) (-> (table/add! table (:actor props-or-actor))
                                   (cell/set-opts! (dissoc props-or-actor :actor)))
         :else (table/add! table props-or-actor)))
      (.row table))
    table)

  (set-opts! [table {:keys [rows cell-defaults] :as opts}]
    (cell/set-opts! (.defaults table) cell-defaults)
    (doto table
      (table/add-rows! rows)
      (set-widget-group-opts! opts))))

(extend-type Window
  window/Ancestor
  (find-ancestor [actor]
    (if-let [parent (actor/parent actor)]
      (if (instance? Window parent)
        parent
        (window/find-ancestor parent))
      (throw (Error. (str "Actor has no parent window " actor))))))
