package trader;

import interfaces.Persistable_IF;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import managers.DatabaseManager_T;
import marketdata.Symbol_T;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.controller.OrderStatus;
import com.ib.controller.Types.Action;

//@Entity( name="Holdings" )  <<<--- SALxx big error, should be the 2 lines below
@Entity
@Table(name="Holdings")
public class Holding_T implements Persistable_IF {
  /* {src_lang=Java}*/
    
    private long id;
    
    private Order order;
    private Contract contract;
    //private OrderState orderState;
    private Symbol_T symbol;
    
    /* the number of shares bought */
    int volume;
    /* the price we bought it at */
    double buy_price;
    
    // TODO: we're going to need an actual_buy_price
    
    /** Specifies the number of shares that have been executed. */
    private int filled;
    /** Specifies the number of shares still outstanding. */
    private int remaining;
    
    /** The average price of the shares that have been executed. This parameter is valid
     *  only if the filled parameter value is greater than zero. Otherwise, the price
     *  parameter will be zero.
     *  
     *  This (may be) is the actual sell price
     */
    private double avgFillPrice = 0;
    /**
     * The last price of the shares that have been executed. This parameter is valid only
     *  if the filled parameter value is greater than zero. Otherwise, the price parameter
     *  will be zero.
     */
    private double lastFillPrice = 0;
    
    /** The order ID of the parent order, used for bracket and auto trailing stop orders. */
    private int parentId;
    
    /** The timestamp that this holding was purchased */
    private Date buyDate = null;
    /** The timestamp that this holding was sold. Null if the holding has not yet been sold */
    private Date sellDate = null;
    
    /** The actual price at which this holding was bought (executed price) */
    private double actualBuyPrice = 0;
    
    /** The desired sell price that we would like to execute a sell order on this holding. */
    /** the actual will be the avg fill price */
    private double sellPrice = 0;
    
    /** the calculate net profit/loss for this holding when it is cashed out **/
    /** this field must be nullable for algorithms to work **/
    private Double net;
    
    /** The amount of money gained by selling this order. If the order has not been sold or was sold at a loss
      set this field to zero. */
    private double gains = 0;
    /** The amount of money lost by selling this order. If the order has not been sold or was sold at a gain
      set this field to zero. */
    private double losses = 0;
        
    /** the returned order status from IB **/
    private String orderStatus = OrderStatus.Unknown.toString();
    
    /**
     * 
     */
    public Holding_T() {
        this.order = new Order();
        this.contract = new Contract();        
    }

    /**
     * 
     */
    public Holding_T(int orderId) {
        this.order = new Order();
        this.order.m_orderId = orderId;
        this.contract = new Contract();
    }
    
    /**
     * @param order
     * @param contract
     */
    public Holding_T(Order order, Contract contract, OrderState orderState) {
        this.order = order;
        this.contract = contract;
        //this.orderState = orderState;
    }

    /**
     * Copy without contract or order
     */
    public Holding_T(int orderId, Holding_T another) {
        this.order = new Order();
        this.order.m_orderId = orderId;
        this.contract = new Contract();
        
        // copy 'important' fields - only retain the fields needed
        // to create a buy or sell order
        this.id = another.id;
        this.setSymbol(another.getSymbol());
        this.volume = another.volume;
        this.buyDate = another.buyDate;
        this.sellDate = another.sellDate;
        this.buy_price = another.buy_price;
        this.actualBuyPrice = another.actualBuyPrice;
        this.sellPrice = another.sellPrice;
        

    }
   
    
    /**
     * The total dollar amount of this holding that have been sold and are now realized gains/losses.
     * 
     * @return realized gains/losses
     */
    @Transient
    public double getSellTotal() {
        return (gains + losses);
    }
    
    //Redundant method
    @Transient
    public double getRealizedGainsLosses() {
        return getSellTotal();
    }
    
    /**
     * @return the id
     */
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    public long getId() {
        return id;
    }
    /**
     * @param id the id to set
     */
    public void setId(long id) {
        this.id = id;
    }
    
    /**
     * @return the order
     */
    @Transient
    public Order getOrder() {
        return order;
    }

    /**
     * @param order the order to set
     */
    public void setOrder(Order order) {
        this.order = order;
    }
    
    @Column ( name = "order_id" )
    public int getOrderId() {
    	//if (order == null) return 0;	//SAL
        return order.m_orderId;
    }
    
    public void setOrderId(int orderId) {
    	//if (this.order != null) {			//SAL
    		this.order.m_orderId = orderId;
    	//}
    }
    
    
    /**
     * Return the buy price that was specified for this holding.
     */
//    @Column( name = "order_buy_price" )
//    public double getBuyPrice() {
//        double buyPrice = 0;
//        
//        if (order.m_auxPrice == 0) {
//            buyPrice = order.m_lmtPrice;
//        } else if (order.m_lmtPrice == 0) {
//            buyPrice = order.m_auxPrice;
//        }
//        return buyPrice;
//    }
//    
//    public void setBuyPrice(double orderBuyPrice) {
//        //Do nothing
//    }
    

    /**
     * Get the last known status of this holding
     * 
     * @return order status
     */
    @Column( name = "order_status" )
    public String getOrderStatus() {
        //String status = OrderStatus.Unknown.toString();
        //if (this.orderState != null) {
        //    status = this.orderState.m_status;
        //}
        
        return orderStatus;
    }
    
    public void setOrderStatus(String status) {
        //if (this.orderState != null) {
        //    this.orderState.m_status = status;
        //}
    	this.orderStatus = status;
    }

    /**
     * @return the contract
     */
    @Transient
    public Contract getContract() {
        return contract;
    }

    /**
     * @param contract the contract to set
     */
    public void setContract(Contract contract) {
        this.contract = contract;
    }

    @Column( name = "volume" )
    public int getVolume() {
        return volume;
    }
    
    public void setVolume(int volume) {
    		this.volume = volume;
    }
    
    @Column( name = "buy_price" )
    public double getBuyPrice() {
        return buy_price;
    }
    
    public void setBuyPrice(double buyPrice) {
    		this.buy_price = buyPrice;
    }
   
    /**
     * @return the orderState
     */
//    @Transient
//    public OrderState getOrderState() {
//       return orderState;
//    }

    /**
     * @param orderState the orderState to set
     */
//    public void setOrderState(OrderState orderState) {
//        this.orderState = orderState;
//    }
    
    /**
     * The ticker symbol of this holding.
     * 
     * @return the symbol
     */
    @Transient
    public Symbol_T getSymbol() {
        return symbol;
    }

    /**
     * @param symbol the symbol to set
     */
    public void setSymbol(Symbol_T symbol) {
        this.symbol = symbol;
    }
    
    @Column( name = "symbol_id" )
    public long getSymbolId() {
        return symbol.getId();
    }
    
    public void setSymbolId(long symbolId) {
    	//SALxx TODO?
    	if (this.symbol == null) { this.symbol = new Symbol_T(symbolId); }
        
    	this.symbol.setId(symbolId);
    }

    public String toString() {
        String str = "OrderID: " + order.m_orderId;
        str = ", Symbol: " + contract.m_symbol;
        //str += ", Status: "  +orderState.m_status;
        str += ", OrderType: " + order.m_orderType;
        
        
        return str;
    }

    @Override
    public long insertOrUpdate() throws HibernateException {
        
        long id = -1;
        Session session = DatabaseManager_T.getSessionFactory().openSession();
        Transaction tx = null;
                
        if (!existsInDB(this)) {
            
            try {
                tx = session.beginTransaction();
                id = (Long) session.save(this);
                tx.commit();
            } catch (HibernateException e) {
                //TODO: for now just print to stdout, we'll change this to a log file later
                e.printStackTrace();
                if (tx != null) tx.rollback();
                throw e;
            } finally {
                session.close();
            }
        } else {
            
            try {
                tx = session.beginTransaction();
                session.update(this);
                id = this.id;
                tx.commit();
            } catch (HibernateException e) {
                //TODO: for now just print to stdout, we'll change this to a log file later
                e.printStackTrace();
                if (tx != null) tx.rollback();
                throw e;
            } finally {
                session.close();
            }
        }
        
        return id;
    }

    @Override
    public void delete() throws HibernateException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void update() throws HibernateException {
    	
        Session session = DatabaseManager_T.getSessionFactory().openSession();
        Transaction tx = null;
 
        try {
            tx = session.beginTransaction();
            session.update(this);
            tx.commit();
        } catch (HibernateException e) {
            //TODO: for now just print to stdout, we'll change this to a log file later
            e.printStackTrace();
            if (tx != null) tx.rollback();
            throw e;
        } finally {
            session.close();
        }       
    }
    
    /**
     * Examine the database to see if this Holding_T object already exists in the DB. Object will be considered duplicates if
     * 1. the have the same id, same symbol_id, same filled #, same remaining # and same status.
     * 
     * @param persistable
     * @return true if the holding is a duplicate, false if not
     */
    @Override
    public boolean existsInDB(Persistable_IF persistable) {
        Holding_T holding = (Holding_T) persistable;
        
        //assume we're going to be insert a duplicate row
        boolean exists = true;
        
    // SALxx- this fails too  - the status object is null!!! and the symbol_id reference was wrong
    // however, since  update doesnt work anyway, ....
    if (1==1) return false;
        
        Session session = DatabaseManager_T.getSessionFactory().openSession();
        Criteria criteria = session.createCriteria(Holding_T.class)
                .add(Restrictions.eq("id", holding.getId()))
                //.add(Restrictions.eq("symbol_id", holding.getSymbolId()))
                .add(Restrictions.eq("symbolId", holding.getSymbolId()))
                .add(Restrictions.eq("filled", holding.filled))
                .add(Restrictions.eq("remaining", holding.getRemaining()));
//SALxx                .add(Restrictions.eq("status", holding.getOrderState().m_status));
        
        //If no symbol was found the consider this MarketData object to be unique and safe to insert into the db

        if (criteria.list().size() == 0) {
            exists = false;
        }
        
        session.close();
        
        return exists;
    }

    /**
     * Update the sell parameters (price and date) for this symbol for the most
     * recent entry for this symbol in the Holdings table.  Its sell date will be null
     * before it is updated - that is the trigger
     * 
     * @param symId
     * @param price
     * @param date
     * @return
     * @throws HibernateException
     */
    // this is because update doesnt work
    //  (also because we need to return a status)
    public int updateSellPosition(long symId, Double price, Date date)  throws HibernateException {

    	Session session = DatabaseManager_T.getSessionFactory().openSession();
        Transaction tx = null;
        
        int nrows = 0;
        
        try {
            tx = session.beginTransaction();
            
          	String hql = "UPDATE trader.Holding_T " +
           			"SET sellPrice = :price, sellDate = :date " +
           			"WHERE symbolId = :sym AND sellDate is null";
           	Query query = session.createQuery(hql);
           	query.setDouble("price", price);
           	query.setTimestamp("date", date);
           	query.setParameter("sym", symId);

        	nrows = query.executeUpdate();
        	
            tx.commit();
        } catch (HibernateException e) {
            //TODO: for now just print to stdout, we'll change this to a log file later
            e.printStackTrace();
            if (tx != null) tx.rollback();
            throw e;
        } finally {
            session.close();
        }
        
        return nrows;
    	
    }

    // also because update doesnt work!!!
    public int updateNet(long id, Double net)  throws HibernateException {
        
    	Session session =  DatabaseManager_T.getSessionFactory().openSession();

    	int nrows = 0;
    	
        // updates must be within a transaction
        Transaction tx = null;
    
        try {
        	tx = session.beginTransaction();

        	String hql = "UPDATE trader.Holding_T " +
        			"SET net = :net " +
        			"WHERE id = :id";
        	Query query = session.createQuery(hql);
        	query.setDouble("net", net);
        	query.setParameter("id", id);

        	nrows = query.executeUpdate();
             
        	tx.commit();
        } catch (HibernateException e) {
        	//TODO: for now just print to stdout, we'll change this to a log file later
        	e.printStackTrace();
        	if (tx != null) tx.rollback();
        } finally {
        	session.close();
        } 
        
        return nrows;
    }
     
    /**
     * update the current holdings with data returned from IB
     * This has logic to determine which date to set based on buy/sell
     * and sets the actual buy price to the fillprice on a buy
     * (avgFillPrice is the actual price on a sell)
     *
     * @return 1 if update, 0 if not
     * @throws HibernateException
     */
    public int updateMarketPosition()  throws HibernateException {
        
    	Session session =  DatabaseManager_T.getSessionFactory().openSession();

    	int nrows = 0;
    	
    	String hql = "UPDATE trader.Holding_T " +
    					"SET orderId = :orderId, " +
    					//"permId = :permId, " +
    					"clientId = :clientId, " +
    					"parentId = :parentId, " +
    					"orderStatus = :status, " +
            			"filled = :filled, " +			// NOTE: these are applicable for both buys and sells
            			"remaining = :remaining, " +
            			"avgFillPrice = :avgFillPrice, ";
						// + "lastFillPrice = :lastFillPrice, ";
    	
    	//if (this.getOrder().m_action.equalsIgnoreCase(Action.BUY.toString()))
    	if (isOwned())
    	{
        	hql += "buyDate = :buyDate, " + 
        		   "actualBuyPrice = :actualBuyPrice ";
    	}
    	else
    	{
        	hql += "sellDate = :sellDate ";
    	}
    	hql += "WHERE id = :id";


        // updates must be within a transaction
        Transaction tx = null;
    
        try {
        	tx = session.beginTransaction();

        	Query query = session.createQuery(hql);
        	
        	query.setInteger("orderId",      this.getOrderId());
        	//query.setInteger("permId",     this.getPermId());
        	query.setInteger("parentId",     this.getParentId());
        	query.setInteger("clientId",     this.getClientId());
        	query.setString ("status",       this.getOrderStatus());
    		query.setInteger("filled",       this.getFilled());
    		query.setInteger("remaining",    this.getRemaining());
    		query.setDouble ("avgFillPrice", this.getAvgFillPrice());
    		
        	//if (this.getOrder().m_action.equalsIgnoreCase(Action.BUY.toString()))
            if (isOwned())
            {
        		query.setTimestamp("buyDate", this.getBuyDate());
        		//query.setDouble("actualBuyPrice", this.getActualBuyPrice());
        		query.setDouble("actualBuyPrice", this.getAvgFillPrice());
        	}
        	else
        	{
        		query.setTimestamp("sellDate", this.getSellDate());
        	}
        	query.setParameter("id", this.id);

        	nrows = query.executeUpdate();
             
        	tx.commit();
        } catch (HibernateException e) {
        	//TODO: for now just print to stdout, we'll change this to a log file later
        	e.printStackTrace();
        	if (tx != null) tx.rollback();
        } finally {
        	session.close();
        } 
        
        return nrows;
    }
    
    /**
     * Specifies the number of shares that have been executed.
     * @return the filled
     */
    @Column( name = "filled" )
    public int getFilled() {
        return filled;
    }

    /**
     * @param filled the filled to set
     */
    public void setFilled(int filled) {
        this.filled = filled;
    }

    /**
     * Specifies the number of shares still outstanding.
     * 
     * @return the @code{remaining}
     */
    @Column( name = "remaining" )
    public int getRemaining() {
        return remaining;
    }

    /**
     * @param remaining the @code{remaining} to set
     */
    public void setRemaining(int remaining) {
        this.remaining = remaining;
    }
    
    /**
     * Get the total number of shares bought and sold for this holding.
     * 
     * @return total number of shares
     */
    @Transient
    public int getNumOfShares() {
        return (filled + remaining);
    }

    /**
     * The average price of the shares that have been executed. This parameter is valid
     *  only if the filled parameter value is greater than zero. Otherwise, the price 
     *  parameter will be zero. This can also be considered as the "buy price".
     *  
     * @return the avgFillPrice
     */
    @Column( name = "avg_fill_price" )
    public double getAvgFillPrice() {
        return avgFillPrice;
    }

    /**
     * @param avgFillPrice the avgFillPrice to set
     */
    public void setAvgFillPrice(double avgFillPrice) {
        this.avgFillPrice = avgFillPrice;
    }

    /**
     * The last price of the shares that have been executed. This parameter is valid only
     *  if the filled parameter value is greater than zero. Otherwise, the price parameter
     *  will be zero.
     *  
     * @return the lastFillPrice
     */
    @Transient							//SALxx - why is this transient?
    //@Column( name = "last_fill_price" )	//SALxx - this is missing from DB
    public double getLastFillPrice() {
        return lastFillPrice;
    }

    /**
     * @param lastFillPrice the lastFillPrice to set
     */
    public void setLastFillPrice(double lastFillPrice) {
        this.lastFillPrice = lastFillPrice;
    }

    /**
     * The order ID of the parent order, used for bracket and auto trailing stop orders.
     * 
     * @return the parentId
     */
    @Column( name = "parent_id" )
    public int getParentId() {
        return parentId;
    }

    /**
     * @param parentId the parentId to set
     */
    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    /**
     * The timestamp that this holding was purchased
     * 
     * @return the buyDate
     */
    @Column( name = "buy_date" )
    @Temporal(value = TemporalType.TIMESTAMP)
    public Date getBuyDate() {
        return buyDate;
    }

    /**
     * @param buyDate the buyDate to set
     */
    public void setBuyDate(Date buyDate) {
        this.buyDate = buyDate;
    }

    /**
     * The timestamp that this holding was sold. Null if the holding has not yet been sold
     * 
     * @return the sellDate
     */
    @Column( name = "sell_date" )
    @Temporal(value = TemporalType.TIMESTAMP)
    public Date getSellDate() {
        return sellDate;
    }

    /**
     * @param sellDate the sellDate to set
     */
    public void setSellDate(Date sellDate) {
        this.sellDate = sellDate;
    }

    /**
     * The actual buy price when the order was executed on this holding.
     * 
     * @return the actulaBuyPrice
     */      	
    @Column ( name = "actual_buy_price" )
    public double getActualBuyPrice() {
        return actualBuyPrice;
    }

    /**
     * @param actualBuyPrice to set
     */
    public void setActualBuyPrice(double actualBuyPrice) {
        this.actualBuyPrice = actualBuyPrice;
    }

    /**
     * The desired sell price that we would like to execute a sell order on this holding.
     * 
     * @return the sellPrice
     */
    @Column ( name = "sell_price" )
    public double getSellPrice() {
        return sellPrice;
    }

    /**
     * @param sellPrice to set
     */
    public void setSellPrice(double sellPrice) {
        this.sellPrice = sellPrice;
    }

    /**
     * The calculated net for this holding.
     * 
     * @return the net
     */
    @Column ( name = "net" )
    public Double getNet() {
        return net;
    }

    /**
     * @param net to set
     */
    public void setNet(Double net) {
        this.net = net;
    }

    /**
     * The amount of money gained by selling this order. If the order has not been sold or was sold at a loss
     * set this field to zero.
     * 
     * @return the gains
     */
    @Transient
    public double getGains() {
        return gains;
    }

    /**
     * @param gains the gains to set
     */
    public void setGains(double gains) {
        this.gains = gains;
    }

    /**
     * The amount of money lost by selling this order. If the order has not been sold or was sold at a gain
     * set this field to zero.
     * 
     * @return the losses
     */
    @Transient
    public double getLosses() {
        return losses;
    }

    /**
     * @param losses the losses to set
     */
    public void setLosses(double losses) {
        this.losses = losses;
    }
    
    @Column( name = "client_id" )
    public int getClientId() {
    	//if (order == null) { return 0; }	//SAL
        return order.m_clientId;
    }
        
    public void setClientId(int clientId) {
    	//if (this.order != null) {			//SAL
    		this.order.m_clientId = clientId;
    	//}
    }

    /**
     * Return true is we currently own this position, otherwise return false
     * 
     * @return the isOwned
     */
    @Transient
    public boolean isOwned() {
        boolean owned = false;
        
        //we own a holding if the buy date is populated, but not the sell date indicating we've bought the position,
        //but haven't sold it yet
        if (buyDate != null && sellDate == null) {
            owned = true;
        }
        
        return owned;
    }

    /**
     * Return true if we have sold this position, otherwise return false
     * 
     */
    @Transient
    public boolean isSold() {
        
        return ((sellDate != null) && (remaining == 0));
    }

    /**
     * Return true if we are in the process of selling this position, otherwise return false
     * 
     */
    @Transient
    public boolean isSelling() {
        
        return ((sellDate != null) && (filled != volume));
    }
  
    /** 
     * are we allowed to sell this holding?
     * 
     * @return true if we own it (bought but not sold) and the volume has been filled
     */
    public boolean canSell() {

    	return (isOwned() && (volume == filled));

    }

}
