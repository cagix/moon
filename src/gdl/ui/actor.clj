(ns gdl.ui.actor)

(defprotocol Actor
  (get-x [_])
  (get-y [_])
  (get-name [_])
  (user-object [_])
  (set-user-object! [_ object])
  (visible? [_])
  (set-visible! [_ visible?])
  (set-touchable! [_ touchable])
  (remove! [_])
  (parent [_])
  (hit [_ [x y]])
  (add-tooltip! [_ tooltip-text]
                "tooltip-text is a (fn [context]) or a string. If it is a function will be-recalculated every show.  Returns the actor.")
  (remove-tooltip! [_])
  (button? [_]
           "Returns true if the actor or its parent is a button.")
  (window-title-bar? [_]
                     "Returns true if the actor is a window title bar.")
  (find-ancestor-window [_])
  (pack-ancestor-window! [_]))

(defn toggle-visible! [actor]
  (set-visible! actor (not (visible? actor))))
