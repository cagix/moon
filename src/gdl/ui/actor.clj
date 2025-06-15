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
  (add-tooltip! [_ tooltip-text]
                "tooltip-text is a (fn [context]) or a string. If it is a function will be-recalculated every show.  Returns the actor.")
  (remove-tooltip! [_]))

(defn toggle-visible! [actor]
  (set-visible! actor (not (visible? actor))))
