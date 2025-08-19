package nurgling.widgets;

import haven.*;
import haven.res.ui.pag.toggle.Toggle;
import nurgling.*;

public class NMenuGridWdg extends Widget
{
    MenuGrid menuGrid;
    private boolean lastSwimmingState = false;
    private boolean lastTrackingState = false;
    private boolean lastCrimeState = false;
    private boolean lastAllowVisitingState = false;

    final Coord marg = UI.scale(new Coord(6,6));
    final Coord dmarg = UI.scale(new Coord(2,2));
    public static final IBox pbox = Window.wbox;
    public NMenuGridWdg()
    {
        super( Coord.z);
    }

    public MenuGrid setMenuGrid(MenuGrid menuGrid)
    {
        this.menuGrid = menuGrid;
        add(menuGrid,dmarg);
        pack();
        return menuGrid;
    }

    @Override
    public void resize(Coord sz)
    {
        super.resize(sz.add(dmarg.mul(2)));
    }

    @Override
    public boolean mousedown(MouseDownEvent ev) {
        if(ui.core.mode!= NCore.Mode.DRAG)
        {
            menuGrid.mousedown(ev);
            return true;
        }
        else
        {
            return super.mousedown(ev);
        }
    }

    @Override
    public boolean mouseup(MouseUpEvent ev) {
        if(ui.core.mode!= NCore.Mode.DRAG)
        {
            return menuGrid.mouseup(ev);
        }
        else
        {
            return super.mouseup(ev);
        }
    }

    @Override
    public void mousemove(MouseMoveEvent ev) {
        super.mousemove(ev);
    }

    @Override
    public void draw(GOut g)
    {
        super.draw(g);
        pbox.draw(g, menuGrid.c.sub(marg), menuGrid.sz.add(marg.mul(2)));
    }
    
    /**
     * Handles toggle-related pagina updates from MenuGrid
     */
    public void handleToggleBuffs(MenuGrid.Pagina pag) {
        if (pag.res instanceof Session.CachedRes.Ref) {
            String ref = ((Session.CachedRes.Ref)pag.res).resnm();
            // Check for toggle state changes and update buff indicators
            if (ref.equals("paginae/act/swim")) {
                checkSwimmingStateChange(pag);
            } else if (ref.equals("paginae/act/tracking")) {
                checkTrackingStateChange(pag);
            } else if (ref.equals("paginae/act/crime")) {
                checkCrimeStateChange(pag);
            } else if (ref.equals("paginae/act/nopeace")) {
                checkAllowVisitingStateChange(pag);
            }
        }
    }
    
    /**
     * Checks if swimming toggle state has changed and updates the buff indicator accordingly
     */
    public void checkSwimmingStateChange(MenuGrid.Pagina pag) {
        try {
            MenuGrid.PagButton button = pag.button();
            if (button instanceof Toggle) {
                Toggle toggle = (Toggle) button;
                boolean currentState = toggle.a;
                
                if (currentState != lastSwimmingState) {
                    lastSwimmingState = currentState;
                    // Notify the game UI about swimming state change
                    if (NUtils.getGameUI() instanceof NGameUI) {
                        ((NGameUI) NUtils.getGameUI()).onSwimmingStateChanged(currentState);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors in swimming state detection
        }
    }
    
    /**
     * Checks if tracking toggle state has changed and updates the buff indicator accordingly
     */
    public void checkTrackingStateChange(MenuGrid.Pagina pag) {
        try {
            MenuGrid.PagButton button = pag.button();
            if (button instanceof Toggle) {
                Toggle toggle = (Toggle) button;
                boolean currentState = toggle.a;
                
                if (currentState != lastTrackingState) {
                    lastTrackingState = currentState;
                    // Notify the game UI about tracking state change
                    if (NUtils.getGameUI() instanceof NGameUI) {
                        ((NGameUI) NUtils.getGameUI()).onTrackingStateChanged(currentState);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors in tracking state detection
        }
    }
    
    /**
     * Checks if crime toggle state has changed and updates the buff indicator accordingly
     */
    public void checkCrimeStateChange(MenuGrid.Pagina pag) {
        try {
            MenuGrid.PagButton button = pag.button();
            if (button instanceof Toggle) {
                Toggle toggle = (Toggle) button;
                boolean currentState = toggle.a;
                
                if (currentState != lastCrimeState) {
                    lastCrimeState = currentState;
                    // Notify the game UI about crime state change
                    if (NUtils.getGameUI() instanceof NGameUI) {
                        ((NGameUI) NUtils.getGameUI()).onCrimeStateChanged(currentState);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors in crime state detection
        }
    }
    
    /**
     * Checks if allow visiting toggle state has changed and updates the buff indicator accordingly
     */
    public void checkAllowVisitingStateChange(MenuGrid.Pagina pag) {
        try {
            MenuGrid.PagButton button = pag.button();
            if (button instanceof Toggle) {
                Toggle toggle = (Toggle) button;
                boolean currentState = toggle.a;
                
                if (currentState != lastAllowVisitingState) {
                    lastAllowVisitingState = currentState;
                    // Notify the game UI about allow visiting state change
                    if (NUtils.getGameUI() instanceof NGameUI) {
                        ((NGameUI) NUtils.getGameUI()).onAllowVisitingStateChanged(currentState);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors in allow visiting state detection
        }
    }
}
