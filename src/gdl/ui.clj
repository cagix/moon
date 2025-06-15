(ns gdl.ui
  (:require [gdl.graphics.texture :as texture]
            [gdl.graphics.g2d.texture-region :as texture-region]
            [gdl.ui.actor :as actor]
            [gdl.ui.group :as group]
            [gdx.graphics.color :as color]
            [gdx.ui :as ui])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.graphics.g2d TextureRegion)
           (com.badlogic.gdx.scenes.scene2d Actor
                                            Group
                                            Stage)
           (com.badlogic.gdx.scenes.scene2d.ui Button
                                               Cell
                                               HorizontalGroup
                                               Image
                                               Label
                                               Table
                                               Stack
                                               Tree$Node
                                               VerticalGroup
                                               WidgetGroup
                                               Window)
           (com.badlogic.gdx.scenes.scene2d.utils BaseDrawable
                                                  ChangeListener
                                                  Drawable
                                                  TextureRegionDrawable)
           (com.badlogic.gdx.utils Align
                                   Scaling)
           (com.kotcrab.vis.ui.widget Separator
                                      VisCheckBox
                                      VisImage
                                      VisImageButton
                                      VisLabel
                                      VisSelectBox
                                      VisScrollPane
                                      VisTable
                                      VisTextButton
                                      VisTextField
                                      VisWindow)))

(defprotocol CanAddActor
  (add! [_ actor]))

(defprotocol CanHit
  (hit [_ [x y]]))

(defprotocol PTable
  (add-rows! [_ rows]
             "rows is a seq of seqs of columns.
             Elements are actors or nil (for just adding empty cells ) or a map of
             {:actor :expand? :bottom?  :colspan int :pad :pad-bottom}. Only :actor is required."))

(defn set-cell-opts! [^Cell cell opts]
  (doseq [[option arg] opts]
    (case option
      :fill-x?    (.fillX     cell)
      :fill-y?    (.fillY     cell)
      :expand?    (.expand    cell)
      :expand-x?  (.expandX   cell)
      :expand-y?  (.expandY   cell)
      :bottom?    (.bottom    cell)
      :colspan    (.colspan   cell (int   arg))
      :pad        (.pad       cell (float arg))
      :pad-top    (.padTop    cell (float arg))
      :pad-bottom (.padBottom cell (float arg))
      :width      (.width     cell (float arg))
      :height     (.height    cell (float arg))
      :center?    (.center    cell)
      :right?     (.right     cell)
      :left?      (.left      cell))))

(defn- set-table-opts! [^Table table {:keys [rows cell-defaults]}]
  (set-cell-opts! (.defaults table) cell-defaults)
  (add-rows! table rows))

(defn- set-opts! [actor opts]
  (ui/set-actor-opts! actor opts)
  (when (instance? Table actor)
    (set-table-opts! actor opts)) ; before widget-group-opts so pack is packing rows
  (when (instance? WidgetGroup actor)
    (ui/set-widget-group-opts actor opts))
  (when (instance? Group actor) ; Check Addable protocol
    (run! #(add! actor %) (:actors opts))) ; or :group/actors ?
  actor)

(defn find-actor-with-id [group id]
  (let [actors (group/children group)
        ids (keep actor/user-object actors)]
    (assert (or (empty? ids)
                (apply distinct? ids)) ; TODO could check @ add
            (str "Actor ids are not distinct: " (vec ids)))
    (first (filter #(= id (actor/user-object %)) actors))))

(defmacro ^:private proxy-ILookup
  "For actors inheriting from Group, implements `clojure.lang.ILookup` (`get`)
  via [find-actor-with-id]."
  [class args]
  `(proxy [~class ILookup] ~args
     (valAt
       ([id#]
        (find-actor-with-id ~'this id#))
       ([id# not-found#]
        (or (find-actor-with-id ~'this id#) not-found#)))))

(defn- -horizontal-group ^HorizontalGroup [{:keys [space pad] :as opts}]
  (let [group (proxy-ILookup HorizontalGroup [])]
    (when space (.space group (float space)))
    (when pad   (.pad   group (float pad)))
    (set-opts! group opts)))

(defn table ^Table [opts]
  (-> (proxy-ILookup VisTable [])
      (set-opts! opts)))

(defn- -stack ^Stack [opts]
  (doto (proxy-ILookup Stack [])
    (set-opts! opts))) ; TODO group opts already has 'actors' ? stack is a group ?

(defn- -check-box
  "on-clicked is a fn of one arg, taking the current isChecked state"
  [{:keys [text on-clicked checked?]}]
  (let [^Button button (VisCheckBox. (str text))]
    (.setChecked button checked?)
    (.addListener button
                  (proxy [ChangeListener] []
                    (changed [event ^Button actor]
                      (on-clicked (.isChecked actor)))))
    button))

(defn label ^VisLabel [{:keys [label/text] :as opts}]
  (doto (VisLabel. ^CharSequence text)
    (set-opts! opts)))

(defn- -text-field [{:keys [text-field/text] :as opts}]
  (-> (VisTextField. (str text))
      (set-opts! opts)))

(defn- -select-box [{:keys [items selected]}]
  (doto (VisSelectBox.)
    (.setItems ^"[Lcom.badlogic.gdx.scenes.scene2d.Actor;" (into-array items))
    (.setSelected selected)))

(defn- -group [opts]
  (doto (proxy-ILookup Group [])
    (set-opts! opts)))

#_(defn- -vertical-group [actors]
    (let [group (proxy-ILookup VerticalGroup [])]
      (run! #(add! group %) actors) ; redundant if we use map based
      group))

; schemas for components would prevents weird errors
; e.g. needs on-clicked ...
(let [type->constructor {:actor.type/actor ui/-actor
                         :actor.type/check-box -check-box
                         :actor.type/group -group
                         :actor.type/horizontal-group -horizontal-group
                         :actor.type/label label
                         :actor.type/select-box -select-box
                         :actor.type/stack -stack
                         :actor.type/table table
                         :actor.type/text-field -text-field
                         :actor.type/widget ui/-widget}]
  (defn -create-actor ^Actor [actor-declaration]
    (try
     (cond
      (instance? Actor actor-declaration) actor-declaration
      (map? actor-declaration) (do
                                (assert (:actor/type actor-declaration))
                                (let [constructor (type->constructor (:actor/type actor-declaration))]
                                  (assert constructor (str "Cannot find constructor for " (:actor/type actor-declaration)))
                                  (constructor actor-declaration)))
      (nil? actor-declaration) nil
      :else (throw (ex-info "Cannot find constructor"
                            {:instance-actor? (instance? Actor actor-declaration)
                             :map? (map? actor-declaration)})))
     (catch Throwable t
       (throw (ex-info "Cannot create-actor"
                       {:actor-declaration actor-declaration}
                       t))))))

(def checked? VisCheckBox/.isChecked)

(def get-selected VisSelectBox/.getSelected)

(defn cells [^Table table]
  (.getCells table))

(defn window ^VisWindow [{:keys [title modal? close-button? center? close-on-escape?] :as opts}]
  (-> (let [window (doto (proxy-ILookup VisWindow [^String title true]) ; true = showWindowBorder
                     (.setModal (boolean modal?)))]
        (when close-button?    (.addCloseButton window))
        (when center?          (.centerWindow   window))
        (when close-on-escape? (.closeOnEscape  window))
        window)
      (set-opts! opts)))

(def get-text VisTextField/.getText)

(defmulti ^:private image* type)

(defmethod image* Drawable [^Drawable drawable]
  (VisImage. drawable))

(defmethod image* Texture [texture]
  (VisImage. (texture/region texture)))

(defmethod image* TextureRegion [texture-region]
  (VisImage. texture-region))

(defn image-widget ; TODO widget also make, for fill parent
  "Takes either a texture-region or drawable. Opts are :scaling, :align and actor opts."
  [object {:keys [scaling align fill-parent?] :as opts}]
  (-> (let [^Image image (image* object)]
        (when (= :center align)
          (.setAlign image Align/center))
        (when (= :fill scaling)
          (.setScaling image Scaling/fill))
        (when fill-parent?
          (.setFillParent image true))
        image)
      (set-opts! opts)))

(defn scroll-pane [actor]
  (doto (VisScrollPane. actor)
    (actor/set-user-object! :scroll-pane)
    (.setFlickScroll false)
    (.setFadeScrollBars false)))

(defn text-button [text on-clicked]
  (doto (VisTextButton. (str text))
    (.addListener (ui/change-listener on-clicked))))

(defn texture-region-drawable [texture-region]
  (TextureRegionDrawable. texture-region))

(defn image-button
  ([texture-region on-clicked]
   (image-button texture-region on-clicked {}))
  ([texture-region on-clicked {:keys [scale]}]
   (let [drawable (texture-region-drawable texture-region)
         button (VisImageButton. ^Drawable drawable)]
     (when scale
       (let [[w h] (texture-region/dimensions texture-region)]
         (BaseDrawable/.setMinSize drawable
                                   (float (* scale w))
                                   (float (* scale h)))))
     (.addListener button (ui/change-listener on-clicked))
     button)))

(defn tree-node ^Tree$Node [actor]
  (proxy [Tree$Node] [actor]))

(defn find-ancestor-window ^Window [actor]
  (if-let [p (actor/parent actor)]
    (if (instance? Window p)
      p
      (find-ancestor-window p))
    (throw (Error. (str "Actor has no parent window " actor)))))

(defn pack-ancestor-window! [actor]
  (.pack (find-ancestor-window actor)))

(defn horizontal-separator-cell [colspan]
  {:actor (Separator. "default")
   :pad-top 2
   :pad-bottom 2
   :colspan colspan
   :fill-x? true
   :expand-x? true})

(defn vertical-separator-cell []
  {:actor (Separator. "vertical")
   :pad-top 2
   :pad-bottom 2
   :fill-y? true
   :expand-y? true})

(defn- button-class? [actor]
  (some #(= Button %) (supers (class actor))))

(defn button?
  "Returns true if the actor or its parent is a button."
  [^Actor actor]
  (or (button-class? actor)
      (and (actor/parent actor)
           (button-class? (actor/parent actor)))))

(defn window-title-bar? ; TODO buggy FIXME
  "Returns true if the actor is a window title bar."
  [^Actor actor]
  (when (instance? Label actor)
    (when-let [p (actor/parent actor)]
      (when-let [p (actor/parent p)]
        (and (instance? VisWindow actor)
             (= (.getTitleLabel ^Window p) actor))))))

(defn create-drawable
  [texture-region
   & {:keys [width
             height
             tint-color]}]
  (let [drawable (doto (texture-region-drawable texture-region)
                   (BaseDrawable/.setMinSize (float width)
                                             (float height)))]
    (if tint-color
      (TextureRegionDrawable/.tint drawable (color/->obj tint-color))
      drawable)))

(defn set-drawable! [image-widget drawable]
  (Image/.setDrawable image-widget drawable))
