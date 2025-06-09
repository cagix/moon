# `:entity/image` -> goal: don't convert but graphics looks up from file memoized and draws

=> GREP `:sprite/` also -> draw-skill-image ...
=> maybe can remove the whole sprite-sheet/sub-sprite/sprite-sheet->sprite shebang

1. cdq.entity.state.player-item-on-cursor
    state/draw-gui-view `:draw/centered`

2. cdq.projectile/size  -> uses sprite [FIXED]

3. cdq.property/image -> converted to texture-region [OK]

4. cdq.render.draw-on-world-viewport.render-entities
    4.1 draw-item-on-cursor-state
        `:draw/centered`

    4.2 draw-skill-image
        `:draw/image`

    4.3 draw-centered-rotated-image
        `:draw/rotated-centered`

5. cdq.ui.action-bar/add-skill! [FIXED]
    `:sprite/texture-region`

6. cdq.ui.windows.inventory
    set-item! -> `:sprite/texture-region` [FIXED]

7. cdq.ui.hp-mana-bar also uses :draw/image [FIXED]

